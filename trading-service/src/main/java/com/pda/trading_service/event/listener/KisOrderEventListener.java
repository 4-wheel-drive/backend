package com.pda.trading_service.event.listener;

import com.pda.common_service.exception.ResourceNotFound;
import com.pda.common_service.repository.KisTokenReader;
import com.pda.common_service.response.ResponseMessage;
import com.pda.trading_service.domain.order.OrderStatus;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.event.OrderCreatedEvent;
import com.pda.trading_service.queue.TradeExecutionQueue;
import com.pda.trading_service.repository.StockOrderRepository;
import com.pda.trading_service.service.dto.OrderEventDto;
import com.pda.trading_service.service.kis.KisOrderService;
import com.pda.trading_service.service.kis.dto.KisOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class KisOrderEventListener {

    private final KisOrderService kisOrderService;
    private final StockOrderRepository stockOrderRepository;
    private final TradeExecutionQueue tradeExecutionQueue;

    @Async
    @EventListener
    @Transactional
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        OrderEventDto dto = event.getOrderDto();
        StockOrder order = stockOrderRepository.findById(dto.orderId())
                .orElseThrow(() -> new ResourceNotFound(ResponseMessage.ORDER_NOT_FOUND));

        try {
            KisOrderResponse response = switch (dto.tradeSide()) {
                case BUY -> kisOrderService.orderBuy(dto);
                case SELL -> kisOrderService.orderSell(dto);
            };

            if (response != null && response.isSuccess()) {
                KisOrderResponse.Output output = response.getOutput();
                String orderNumber = output.getOrderNumber();

                log.info("[모의투자 주문 성공] 주문번호: {}, 시간: {}", orderNumber, output.getOrderTime());

                order.updateStatus(OrderStatus.CREATED);
                order.updateTradeId(orderNumber);
                stockOrderRepository.save(order);
                tradeExecutionQueue.enqueue(order);
            }

        } catch (Exception e) {
            log.error("[모의투자 주문 중 예외 발생] {}", e.getMessage(), e);
            order.updateStatus(OrderStatus.FAIL);
            stockOrderRepository.save(order);
        }
    }
}
