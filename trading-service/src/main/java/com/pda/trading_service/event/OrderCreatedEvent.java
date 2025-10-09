package com.pda.trading_service.event;

import com.pda.trading_service.service.dto.OrderEventDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderCreatedEvent extends ApplicationEvent {

    private final OrderEventDto orderDto;

    public OrderCreatedEvent(Object source, OrderEventDto orderDto) {
        super(source);
        this.orderDto = orderDto;
    }
}