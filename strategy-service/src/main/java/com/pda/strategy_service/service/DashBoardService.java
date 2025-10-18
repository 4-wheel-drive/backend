package com.pda.strategy_service.service;

import com.pda.strategy_service.controller.dto.DashBoardResponse.GetProfitRate;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetRanking;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetStockProfit;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetStocks;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetTransactions;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetTransactionsByStock;
import com.pda.strategy_service.controller.dto.KisPsblOrderResponse;
import com.pda.strategy_service.controller.dto.OrderPossibleBalanceResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

public interface DashBoardService {
    GetProfitRate getProfitRate(Long memberId);
    GetRanking getRanking(Long memberId);
    GetStocks getStocks(Long memberId);
    GetStockProfit getStocksProfit(Long memberId);
    GetTransactions getTransactions(Long memberId, Pageable pageable);
    GetTransactionsByStock getTransactionsByStock(Long memberId, String stockCode);
    OrderPossibleBalanceResponse getAvailableCash(Long memberId);
}