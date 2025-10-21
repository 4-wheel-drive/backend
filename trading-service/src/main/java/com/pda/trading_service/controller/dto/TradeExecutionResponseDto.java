package com.pda.trading_service.controller.dto;

import com.pda.trading_service.domain.execution.dto.TradeExecutionDto;
import java.util.List;

public class TradeExecutionResponseDto {

    public record ReadTradeExecution(
            Long tradeExecutionCount,
            PageInfo pageInfo,
            List<TradeExecutionDto> tradeExecutions
    ) {
        public static ReadTradeExecution of(
                List<TradeExecutionDto> tradeExecutions,
                int totalPages,
                long totalElements,
                int currentPage,
                int pageSize
        ) {
            return new ReadTradeExecution(
                    totalElements,
                    new PageInfo(currentPage, totalPages, totalElements, pageSize),
                    tradeExecutions
            );
        }
    }

    public record PageInfo(
            int currentPage,
            int totalPages,
            long totalElements,
            int size
    ) {}
}
