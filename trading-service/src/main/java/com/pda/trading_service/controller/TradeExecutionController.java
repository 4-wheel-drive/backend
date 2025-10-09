package com.pda.trading_service.controller;

import com.pda.common_service.response.ApiResponse;
import com.pda.common_service.response.ResponseMessage;
import com.pda.trading_service.controller.dto.TradeExecutionResponseDto.ReadTradeExecution;
import com.pda.trading_service.service.TradeExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trade/execution")
public class TradeExecutionController {
    private final TradeExecutionService tradeExecutionService;

    @GetMapping("/{strategyId}")
    public ResponseEntity<ApiResponse<ReadTradeExecution>> getTradeExecution(@PathVariable Long strategyId) {
        Long memberId = 1L;
        ReadTradeExecution tradeExecutions = tradeExecutionService.getTradeExecution(memberId, strategyId);
        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_EXECUTION_SUCCESS.getCode(),
                        ResponseMessage.GET_EXECUTION_SUCCESS.getMessage(),
                        tradeExecutions));
    }
}

