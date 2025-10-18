package com.pda.strategy_service.controller;

import com.pda.common_service.authentication.Accessor;
import com.pda.common_service.authentication.Auth;
import com.pda.common_service.authentication.MemberOnly;
import com.pda.common_service.response.ApiResponse;
import com.pda.common_service.response.ResponseMessage;
import com.pda.strategy_service.controller.dto.StockResponse.ReadStocks;
import com.pda.strategy_service.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<ApiResponse<ReadStocks>> getAllStocks() {
        ReadStocks readStocks = stockService.getAllStocks();

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_ALL_STOCKS_SUCCESS.getCode(),
                        ResponseMessage.GET_ALL_STOCKS_SUCCESS.getMessage(),
                        readStocks));
    }

    @GetMapping("/my-stocks")
    public ResponseEntity<ApiResponse<ReadStocks>> getMyStocks() {
        Long memberId = 1L;

        ReadStocks readStocks = stockService.getMyStocks(memberId);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_MY_STOCKS_SUCCESS.getCode(),
                        ResponseMessage.GET_MY_STOCKS_SUCCESS.getMessage(),
                        readStocks));
    }
}
