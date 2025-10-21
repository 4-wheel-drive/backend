package com.pda.trading_service.controller;

import com.pda.common_service.response.ApiResponse;
import com.pda.common_service.response.ResponseMessage;
import com.pda.trading_service.controller.dto.TradeExecutionResponseDto.ReadTradeExecution;
import com.pda.trading_service.service.TradeExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trade/execution")
public class TradeExecutionController {
    private final TradeExecutionService tradeExecutionService;

    @GetMapping("/{strategyId}")
    public ResponseEntity<ApiResponse<ReadTradeExecution>> getTradeExecutions(
            @PathVariable Long strategyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Long memberId = 1L;
        ReadTradeExecution executions = tradeExecutionService.getTradeExecutions(memberId, strategyId, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                ResponseMessage.GET_EXECUTION_SUCCESS.getCode(),
                ResponseMessage.GET_EXECUTION_SUCCESS.getMessage(),
                executions
        ));
    }
}

