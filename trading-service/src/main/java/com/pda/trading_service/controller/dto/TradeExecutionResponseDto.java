package com.pda.trading_service.controller.dto;

import com.pda.trading_service.domain.execution.dto.TradeExecutionDto;
import java.util.List;

public class TradeExecutionResponseDto {
    public record ReadTradeExecution(
            Integer tradeExecutionCount,
            List<TradeExecutionDto> tradeExecutions
    ) {
    }
}
