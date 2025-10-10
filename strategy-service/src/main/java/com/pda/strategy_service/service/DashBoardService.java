package com.pda.strategy_service.service;

import com.pda.strategy_service.controller.dto.DashBoardResponse.GetProfitRate;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetRanking;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetStockProfit;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetStocks;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetTransactions;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetTransactionsByStock;
import org.springframework.data.domain.Pageable;

public interface DashBoardService {
    GetProfitRate getProfitRate(Long memberId);
    GetRanking getRanking(Long memberId);
    GetStocks getStocks(Long memberId);
    GetStockProfit getStocksProfit(Long memberId);
    GetTransactions getTransactions(Long memberId, Pageable pageable);
    GetTransactionsByStock getTransactionsByStock(Long memberId, String stockCode);
}