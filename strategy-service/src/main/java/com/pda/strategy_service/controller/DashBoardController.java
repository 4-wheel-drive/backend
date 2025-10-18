package com.pda.strategy_service.controller;

import com.pda.common_service.response.ApiResponse;
import com.pda.common_service.response.ResponseMessage;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetProfitRate;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetRanking;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetStockProfit;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetStocks;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetTransactions;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetTransactionsByStock;
import com.pda.strategy_service.controller.dto.KisPsblOrderResponse;
import com.pda.strategy_service.controller.dto.OrderPossibleBalanceResponse;
import com.pda.strategy_service.service.DashBoardService;
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
@RequestMapping("/api/v1/accounts")
public class DashBoardController {
    private final DashBoardService dashBoardService;

//    @MemberOnly
    @GetMapping("/profit-rate")
//    public ResponseEntity<ApiResponse<GetProfitRate>> getProfitRate(@Auth Accessor accessor) {
    public ResponseEntity<ApiResponse<GetProfitRate>> getProfitRate() {
        Long memberId = 1L;
        GetProfitRate profitRate = dashBoardService.getProfitRate(memberId);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_RETURNS_SUCCESS.getCode(),
                        ResponseMessage.GET_RETURNS_SUCCESS.getMessage(),
                        profitRate));
    }

//    @MemberOnly
    @GetMapping("/profit-rate/ranking")
//    public ResponseEntity<ApiResponse<GetRanking>> getRanking(@Auth Accessor accessor) {
    public ResponseEntity<ApiResponse<GetRanking>> getRanking() {
        Long memberId = 1L;
        GetRanking ranking = dashBoardService.getRanking(memberId);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_PROFIT_RATE_RANKING_SUCCESS.getCode(),
                        ResponseMessage.GET_PROFIT_RATE_RANKING_SUCCESS.getMessage(),
                        ranking));
    }

//    @MemberOnly
    @GetMapping("/stocks")
//    public ResponseEntity<ApiResponse<GetStocks>> getStocks(@Auth Accessor accessor) {
    public ResponseEntity<ApiResponse<GetStocks>> getStocks() {
        Long memberId = 1L;
        GetStocks stocks = dashBoardService.getStocks(memberId);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_STOCKS_SUCCESS.getCode(),
                        ResponseMessage.GET_STOCKS_SUCCESS.getMessage(),
                        stocks));
    }

//    @MemberOnly
    @GetMapping("/stocks/profit-rate")
//    public ResponseEntity<ApiResponse<GetStockProfit>> getStocksProfit(@Auth Accessor accessor) {
    public ResponseEntity<ApiResponse<GetStockProfit>> getStocksProfit() {
        Long memberId = 1L;
        GetStockProfit stocksProfit = dashBoardService.getStocksProfit(memberId);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_STOCKS_PROFIT_SUCCESS.getCode(),
                        ResponseMessage.GET_STOCKS_PROFIT_SUCCESS.getMessage(),
                        stocksProfit));
    }

//    @MemberOnly
    @GetMapping("/transactions")
//    public ResponseEntity<ApiResponse<GetTransactions>> getTransactions(@Auth Accessor accessor, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    public ResponseEntity<ApiResponse<GetTransactions>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(page, size);
        GetTransactions transactions = dashBoardService.getTransactions(memberId, pageable);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_TRANSACTIONS_SUCCESS.getCode(),
                        ResponseMessage.GET_TRANSACTIONS_SUCCESS.getMessage(),
                        transactions));
    }

//    @MemberOnly
    @GetMapping("/transactions/{stockCode}")
//    public ResponseEntity<ApiResponse<GetTransactionsByStock>> getTransactionsByStock(@Auth Accessor accessor, @PathVariable String stockCode) {
    public ResponseEntity<ApiResponse<GetTransactionsByStock>> getTransactionsByStock(@PathVariable String stockCode) {
        Long memberId = 1L;
        GetTransactionsByStock transactions = dashBoardService.getTransactionsByStock(memberId, stockCode);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_TRANSACTIONS_BY_STOCK_SUCCESS.getCode(),
                        ResponseMessage.GET_TRANSACTIONS_BY_STOCK_SUCCESS.getMessage(),
                        transactions));
    }

    @GetMapping("")
    public ResponseEntity<ApiResponse<OrderPossibleBalanceResponse>> getOrderPossibleBalance() {
        Long memberId = 1L;
        OrderPossibleBalanceResponse orderPossibleBalanceResponse = dashBoardService.getAvailableCash(memberId);
        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_POSSIBLE_BALANCE_SUCCESS.getCode(),
                        ResponseMessage.GET_POSSIBLE_BALANCE_SUCCESS.getMessage(),
                        orderPossibleBalanceResponse));
    }
}
