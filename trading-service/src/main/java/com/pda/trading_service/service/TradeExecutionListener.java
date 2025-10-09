package com.pda.trading_service.service;

import com.pda.common_service.exception.ResourceNotFound;
import com.pda.common_service.response.ResponseMessage;
import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.execution.TradeExecutionStatus;
import com.pda.trading_service.domain.order.OrderStatus;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.repository.StockOrderRepository;
import com.pda.trading_service.repository.TradeExecutionRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeExecutionListener {

    private final StockOrderRepository stockOrderRepository;
    private final TradeExecutionRepository tradeExecutionRepository;

    /**
     * 체결 이벤트 수신 후 DB 업데이트
     *
     * @return true = 체결 완료되어 소켓 종료 신호
     */
    @Transactional
    public boolean onExecutionMessage(String orderNo, String payload) {
        try {
            // KIS 체결 이벤트 파싱
            if (!payload.contains("H0STCNI0")) {
                return false;
            }

            String[] parts = payload.split("\\|", -1);
            if (parts.length < 6) {
                return false;
            }

            String recvOrderNo = parts[1]; // 주문번호
            String filledQty = parts[5];
            String filledPrice = parts[6];

            if (!recvOrderNo.equals(orderNo)) {
                return false;
            }

            StockOrder order = stockOrderRepository.findByTradeId(orderNo)
                    .orElseThrow(() -> new ResourceNotFound(
                            ResponseMessage.ORDER_NOT_FOUND));

            TradeExecution execution = TradeExecution.builder()
                    .stockOrder(order)
                    .tradeSide(order.getTradeSide())
                    .quantity(Integer.parseInt(filledQty))
                    .price(new BigDecimal(filledPrice))
                    .status(TradeExecutionStatus.FILLED)
                    .build();

            tradeExecutionRepository.save(execution);

            order.updateStatus(OrderStatus.FILLED);
            stockOrderRepository.save(order);

            log.info("[체결 완료] orderNo={}, qty={}, price={}", recvOrderNo, filledQty, filledPrice);
            return true;

        } catch (Exception e) {
            log.error("체결 처리 중 오류", e);
            return false;
        }
    }
}
