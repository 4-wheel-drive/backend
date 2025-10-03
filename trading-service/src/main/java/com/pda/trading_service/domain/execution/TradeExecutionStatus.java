package com.pda.trading_service.domain.execution;

public enum TradeExecutionStatus {
    PENDING,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED
}
