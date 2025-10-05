package com.pda.trading_service.domain.execution;

import java.util.EnumSet;
import java.util.Set;

public enum TradeExecutionStatus {
    PENDING,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED;

    public static final Set<TradeExecutionStatus> FILLED_STATES =
            EnumSet.of(FILLED, PARTIALLY_FILLED);
}
