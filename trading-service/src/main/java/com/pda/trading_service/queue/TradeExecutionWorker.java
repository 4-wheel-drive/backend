package com.pda.trading_service.queue;

import com.pda.trading_service.config.StrategyModuleClient;
import com.pda.trading_service.controller.dto.StrategyWithMemberDto;
import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.execution.TradeExecutionStatus;
import com.pda.trading_service.domain.order.OrderStatus;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.repository.TradeExecutionRepository;
import com.pda.trading_service.service.kis.KisTradeExecutionService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeExecutionWorker {

    private final TradeExecutionQueue tradeExecutionQueue;
    private final TradeExecutionRepository tradeExecutionRepository;
    private final StrategyModuleClient strategyModuleClient;
    private final KisTradeExecutionService kisTradeExecutionService;
    private final Map<Long, Integer> retryCount = new ConcurrentHashMap<>();

    private static final int MAX_RETRY = 10;       // 최대 10회 재시도
    private static final long RETRY_DELAY_MS = 10_000; // 10초 후 재시도

    @PostConstruct
    public void startWorker() {
        Thread worker = new Thread(this::processQueue, "trade-execution-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("✅ TradeExecutionWorker 시작됨 (백그라운드 큐 소비)");
    }

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public void processQueue() {
        while (true) {
            try {
                StockOrder order = tradeExecutionQueue.dequeue();
                handleOrder(order);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ TradeExecutionWorker 인터럽트 감지 → 종료");
                break;
            } catch (Exception e) {
                log.error("❌ 큐 처리 중 오류 발생: {}", e.getMessage(), e);
            }
        }
    }

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    protected void handleOrder(StockOrder order) {
        try {
            // 🔒 이미 체결된 주문이라면 즉시 스킵
            if (tradeExecutionRepository.existsByStockOrder(order)) {
                log.debug("⏸ 주문({}) 이미 체결됨 → 스킵", order.getTradeId());
                retryCount.remove(order.getId());
                return;
            }

            StrategyWithMemberDto strategyInfo = strategyModuleClient.getStrategyInfo(order.getStrategyId());
            TradeExecution execution = kisTradeExecutionService.checkTradeExecution(order, LocalDate.now(), strategyInfo);

            if (execution == null) {
                int count = retryCount.getOrDefault(order.getId(), 0);
                if (count < MAX_RETRY) {
                    retryCount.put(order.getId(), count + 1);
                    log.info("⌛ 주문({}) 미체결 → {}회차 재시도 예정 ({}초 후)", order.getTradeId(), count + 1, RETRY_DELAY_MS / 1000);
                    Thread.sleep(RETRY_DELAY_MS);
                    tradeExecutionQueue.enqueue(order);
                } else {
                    log.warn("🚫 주문({}) 최대 재시도({}) 초과 → 포기", order.getTradeId(), MAX_RETRY);
                    retryCount.remove(order.getId());
                }
                return;
            }

            // ✅ 중복 INSERT 방지 (DB 유니크 제약 + 예외 핸들링)
            try {
                execution.setStockOrder(order);
                tradeExecutionRepository.save(execution);
            } catch (DataIntegrityViolationException e) {
                log.warn("⚠️ 중복 체결 시도 감지 (DB 제약 위반) 주문({}) → 무시", order.getTradeId());
                retryCount.remove(order.getId());
                return; // rollback 없음
            }

            if (execution.getStatus() == TradeExecutionStatus.FILLED) {
                order.updateStatus(OrderStatus.FILLED);
                retryCount.remove(order.getId());
                log.info("✅ 주문({}) 체결 완료 (수량: {}, 가격: {})",
                        order.getTradeId(), execution.getQuantity(), execution.getPrice());
            }

        } catch (DataIntegrityViolationException e) {
            log.warn("⚠️ 주문({}) 중복 체결 감지 → 무시", order.getTradeId());
            retryCount.remove(order.getId());
        } catch (Exception e) {
            log.error("❌ 주문({}) 처리 중 예외 발생: {}", order.getTradeId(), e.getMessage(), e);
            retryCount.remove(order.getId());
        }
    }
}
