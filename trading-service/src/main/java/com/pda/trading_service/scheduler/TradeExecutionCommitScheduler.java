package com.pda.trading_service.scheduler;

import com.pda.trading_service.config.StrategyModuleClient;
import com.pda.trading_service.controller.dto.StrategyWithMemberDto;
import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.execution.TradeExecutionStatus;
import com.pda.trading_service.domain.order.OrderStatus;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.repository.StockOrderRepository;
import com.pda.trading_service.repository.TradeExecutionRepository;
import com.pda.trading_service.service.kis.KisTradeExecutionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeExecutionCommitScheduler {

    private final StockOrderRepository stockOrderRepository;
    private final TradeExecutionRepository tradeExecutionRepository;
    private final KisTradeExecutionService kisTradeExecutionService;
    private final StrategyModuleClient strategyModuleClient;

    @Scheduled(cron = "0 */2 * * * *", zone = "Asia/Seoul")
    public void executeTradeCheck() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime marketOpen = LocalTime.of(9, 0);
        LocalTime marketClose = LocalTime.of(15, 40);

        // 장 시간 외에는 스케줄 종료
        if (now.isBefore(marketOpen) || now.isAfter(marketClose)) {
            log.info("⏸ 장외 시간 ({}), 체결 확인 스킵", now);
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<StockOrder> createdOrders = stockOrderRepository.findByStatusAndCreateAtToday(OrderStatus.CREATED.name(),
                startOfDay, endOfDay);

        if (createdOrders.isEmpty()) {
            log.info("[체결 확인] 대기 주문 없음");
            return;
        }

        log.info("[체결 확인] 시작 ({}건)", createdOrders.size());

        for (StockOrder order : createdOrders) {
            handleSingleOrder(order, today);
        }
    }

    /**
     * 개별 주문에 대한 체결 확인 및 저장 로직
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSingleOrder(StockOrder order, LocalDate today) {
        try {
            if (tradeExecutionRepository.findByStockOrder(order).isPresent()) {
                log.debug("⏸ 주문({}) 이미 체결 정보 존재 → 스킵", order.getTradeId());
                return;
            }

            StrategyWithMemberDto strategyInfo = strategyModuleClient.getStrategyInfo(order.getStrategyId());
            TradeExecution execution = kisTradeExecutionService.checkTradeExecution(order, today, strategyInfo);

            if (execution == null) {
                log.debug("⏸ 주문({}) 미체결 상태 → 스킵", order.getTradeId());
                return;
            }

            execution.setStockOrder(order);
            tradeExecutionRepository.save(execution);
            log.info("💾 주문({}) 체결 정보 저장 완료", order.getTradeId());

            if (execution.getStatus() == TradeExecutionStatus.FILLED) {
                order.updateStatus(OrderStatus.FILLED);
                stockOrderRepository.save(order);
                log.info("주문({}) 완전 체결 (수량: {})", order.getTradeId(), execution.getQuantity());
            }

        } catch (DataIntegrityViolationException e) {
            log.warn("⚠️ 주문({}) 중복 체결 감지(DB 유니크 제약)", order.getTradeId());
        } catch (Exception e) {
            log.error("❌ 주문({}) 체결 확인 중 오류: {}", order.getTradeId(), e.getMessage(), e);
        }
    }

}
