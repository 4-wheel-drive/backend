package com.pda.strategy_service.controller.dto;

import com.pda.strategy_service.service.dto.ProfitPoint;
import java.math.BigDecimal;
import java.util.List;

public class DashBoardResponse {
    public record GetProfitRate(
            ProfitSeriesData profitSeries
    ) {
    }

    public record ProfitSeriesData(
            List<ProfitPoint> oneMonth,
            List<ProfitPoint> threeMonth,
            List<ProfitPoint> sixMonth,
            List<ProfitPoint> oneYear,
            List<ProfitPoint> all
    ) {
    }

    public record GetRanking(
            String accountId,
            List<RankingItem> items
    ) {
    }

    public record RankingItem(
            Integer rank,
            String stockCode,
            String stockName,
            Integer qty,
            BigDecimal marketValue,
            BigDecimal costBasis,
            BigDecimal pnl,
            BigDecimal profitRate
    ) {
    }

    public record GetStocks(
            String accountId,
            BigDecimal totalMarketValue,
            List<StockItem> items
    ) {
    }

    public record StockItem(
            String stockCode,
            String stockName,
            BigDecimal marketValue,
            Integer qty,
            BigDecimal weight
    ) {
    }

    public record GetStockProfit(
            String accountId,
            String asOf,
            List<StockProfitData> stockData
    ) {
    }

    public record StockProfitData(
            String stockCode,
            String stockName,
            Integer qty,
            BigDecimal avgBuyPrice,
            BigDecimal amount,
            BigDecimal profitRate
    ) {
    }

    public record GetTransactions(
            String accountId,
            List<TransactionItem> items,
            PageInfo pageInfo
    ) {
    }

    public record PageInfo(
            Integer currentPage,
            Integer totalPages,
            Long totalElements,
            Integer size
    ) {
    }

    public record TransactionItem(
            String transactionId,
            String executionTime,
            String side,
            String stockCode,
            String stockName,
            StrategyInfo strategyInfo,
            Integer qty,
            BigDecimal price
    ) {
    }

    public record StrategyInfo(
            Long id,
            String strategyName
    ) {
    }

    public record GetTransactionsByStock(
            String accountId,
            String stockCode,
            String stockName,
            List<TransactionByStockItem> items
    ) {
    }

    public record TransactionByStockItem(
            String transactionId,
            String executionTime,
            String side,
            StrategyInfo strategyInfo,
            Integer qty,
            BigDecimal price,
            BigDecimal amount
    ) {
    }
}
