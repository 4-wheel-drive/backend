package com.pda.trading_service.domain.execution.dto;

import com.pda.trading_service.domain.execution.TradeExecutionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeExecutionDto(
        Long id,
        TradeExecutionType tradeExecutionType,
        Integer tradeExecutionQuantity,
        BigDecimal tradeExecutionPrice,
        LocalDateTime executionTime
) {
}
