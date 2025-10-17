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
    @Transactional
    public void executeTradeCheck() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<StockOrder> createdOrders = stockOrderRepository.findByStatusAndCreateAtToday(
                OrderStatus.CREATED.name(), startOfDay, endOfDay);

        if (createdOrders.isEmpty()) {
            log.info("체결 대기 주문이 없습니다.");
            return;
        }

        log.info("체결 확인 시작 ({}건)", createdOrders.size());

        for (StockOrder order : createdOrders) {
            try {
                StrategyWithMemberDto strategyInfo = strategyModuleClient.getStrategyInfo(order.getStrategyId());
                TradeExecution execution = kisTradeExecutionService.checkTradeExecution(order, today, strategyInfo);

                tradeExecutionRepository.save(execution);

                if (execution.getStatus() == TradeExecutionStatus.FILLED) {
                    order.updateStatus(OrderStatus.FILLED);
                    stockOrderRepository.save(order);
                }

                log.info("주문({}) 상태={}, 체결수량={}",
                        order.getTradeId(), execution.getStatus(), execution.getQuantity());

            } catch (Exception e) {
                log.error("주문({}) 체결 확인 중 오류: {}", order.getTradeId(), e.getMessage());
            }
        }
    }
}
