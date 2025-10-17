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
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 매 1분마다 CREATED 주문을 조회해서 체결 상태 확인 후 저장
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void executeTradeCheck() {
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
            StrategyWithMemberDto strategyInfo = strategyModuleClient.getStrategyInfo(order.getStrategyId());
            TradeExecution execution = kisTradeExecutionService.checkTradeExecution(order, today, strategyInfo);

            if (execution == null) {
                log.debug("⏸주문({}) 미체결 상태 → 스킵", order.getTradeId());
                return;
            }

            // 이미 동일 주문번호로 체결된 건 중복 방지
            boolean exists = tradeExecutionRepository.findByStockOrder(order).isPresent();
            if (exists) {
                log.info("주문({}) 이미 체결 처리됨 → 스킵", order.getTradeId());
                return;
            }

            // 체결 정보 저장
            tradeExecutionRepository.save(execution);

            if (execution.getStatus() == TradeExecutionStatus.FILLED) {
                order.updateStatus(OrderStatus.FILLED);
                stockOrderRepository.save(order);
                log.info("주문({}) 완전 체결 완료 (수량: {})", order.getTradeId(), execution.getQuantity());
            }
        } catch (Exception e) {
            log.error("주문({}) 체결 확인 중 오류: {}", order.getTradeId(), e.getMessage(), e);
        }
    }
}
