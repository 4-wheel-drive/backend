package com.pda.trading_service.controller.dto;

import com.pda.trading_service.domain.execution.dto.TradeExecutionDto;
import java.util.List;

public class TradeExecutionResponseDto {

    public record ReadTradeExecution(
            Integer tradeExecutionCount,   // 현재 페이지의 데이터 개수
            PageInfo pageInfo,             // 페이지 정보
            List<TradeExecutionDto> tradeExecutions // 실제 데이터
    ) {
        public static ReadTradeExecution of(
                List<TradeExecutionDto> tradeExecutions,
                int totalPages,
                long totalElements,
                int currentPage,
                int pageSize
        ) {
            return new ReadTradeExecution(
                    tradeExecutions.size(),
                    new PageInfo(currentPage, totalPages, totalElements, pageSize),
                    tradeExecutions
            );
        }
    }

    public record PageInfo(
            int currentPage,     // 현재 페이지 번호
            int totalPages,      // 전체 페이지 수
            long totalElements,  // 전체 데이터 수
            int size             // 한 페이지당 데이터 개수
    ) {}
}
