package com.pda.trading_service.controller;

import com.pda.trading_service.service.TradeExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trade/execution")
public class TradeExecutionController {
    private final TradeExecutionService tradeExecutionService;

//    @GetMapping()
//    public ResponseEntity<ApiResponse<Object>> getTradeExecution() {
//        Long memberId = 1L;
//        tradeExecutionService.
//        return ResponseEntity
//                .ok()
//                .body(ApiResponse.success(
//                        ResponseMessage.GET_STRATEGIES_SUCCESS.getCode(),
//                        ResponseMessage.GET_STRATEGIES_SUCCESS.getMessage(),
//                        null));
//    }

}
