package com.pda.trading_service.event;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class TradeFilledEvent {
    private final Long strategyId;
    private final LocalDateTime timestamp;

    public TradeFilledEvent(Object source, Long strategyId) {
        super();
        this.strategyId = strategyId;
        this.timestamp = LocalDateTime.now();
    }
}

