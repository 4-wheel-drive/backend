package com.pda.strategy_service.service;

import com.pda.common_service.exception.MemberException;
import com.pda.common_service.exception.StrategyException;
import com.pda.common_service.response.ResponseMessage;
import com.pda.common_service.stock.Stock;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.repository.MemberRepository;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetProfitRate;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetRanking;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetStockProfit;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetStocks;
import com.pda.strategy_service.controller.dto.DashBoardResponse.ProfitSeriesData;
import com.pda.strategy_service.controller.dto.DashBoardResponse.RankingItem;
import com.pda.strategy_service.controller.dto.DashBoardResponse.StockItem;
import com.pda.strategy_service.controller.dto.DashBoardResponse.StockProfitData;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetTransactions;
import com.pda.strategy_service.controller.dto.DashBoardResponse.PageInfo;
import com.pda.strategy_service.controller.dto.DashBoardResponse.StrategyInfo;
import com.pda.strategy_service.controller.dto.DashBoardResponse.TransactionItem;
import com.pda.strategy_service.controller.dto.DashBoardResponse.GetTransactionsByStock;
import com.pda.strategy_service.controller.dto.DashBoardResponse.TransactionByStockItem;
import com.pda.strategy_service.controller.dto.StrategyResponse.ProfitSeries;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.StrategyExistedStatus;
import com.pda.strategy_service.domain.Transaction;
import com.pda.strategy_service.repository.jpa.StrategyRepository;
import com.pda.strategy_service.repository.jpa.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashBoardServiceImpl implements DashBoardService {
    private final MemberRepository memberRepository;
    private final StrategyRepository strategyRepository;
    private final TransactionRepository transactionRepository;
    private final ProfitCalculator profitCalculator;

    @Override
    public GetProfitRate getProfitRate(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        List<Strategy> strategies = strategyRepository.findAllByMemberAndStrategyExistedStatus(member,
                StrategyExistedStatus.EXISTED);

        ProfitSeries profitSeries = profitCalculator.getAllPeriodSeriesForAccount(strategies);

        ProfitSeriesData profitSeriesData = new ProfitSeriesData(
                profitSeries.oneMonth(),
                profitSeries.threeMonth(),
                profitSeries.sixMonth(),
                profitSeries.oneYear(),
                profitSeries.all()
        );

        return new GetProfitRate(profitSeriesData);
    }

    @Override
    public GetRanking getRanking(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        List<Strategy> strategies = strategyRepository.findAllByMemberAndStrategyExistedStatus(member,
                StrategyExistedStatus.EXISTED);

        Map<String, Strategy> stockMap = strategies.stream()
                .filter(s -> s.getStock() != null)
                .filter(s -> s.getStrategyProfitSummary() != null)
                .collect(Collectors.toMap(
                        s -> s.getStock().toDto().stockCode(),
                        s -> s,
                        (existing, replacement) -> existing
                ));

        List<RankingItem> tempItems = stockMap.values().stream()
                .map(strategy -> {
                    Stock stock = strategy.getStock();
                    var stockInfo = stock.toDto();
                    String stockCode = stockInfo.stockCode();

                    Integer totalQty = transactionRepository.calculateStockQuantityByMember(member, stockCode);
                    BigDecimal netInvestment = transactionRepository.calculateNetInvestmentByMember(member, stockCode);
                    BigDecimal currentPrice = strategy.getStrategyProfitSummary()
                            .getStrategyProfitSummaryCurrentPrice();
                    BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(totalQty));

                    BigDecimal profitRate = netInvestment.compareTo(BigDecimal.ZERO) > 0
                            ? marketValue.subtract(netInvestment)
                            .divide(netInvestment, 2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    BigDecimal pnl = marketValue.subtract(netInvestment);

                    return new RankingItem(
                            0,
                            stockCode,
                            stockInfo.stockName(),
                            totalQty,
                            marketValue.setScale(2, java.math.RoundingMode.HALF_UP),
                            netInvestment.setScale(2, java.math.RoundingMode.HALF_UP),
                            pnl.setScale(2, java.math.RoundingMode.HALF_UP),
                            profitRate.setScale(2, java.math.RoundingMode.HALF_UP)
                    );
                })
                .sorted(Comparator.comparing(RankingItem::profitRate).reversed())
                .limit(10)
                .toList();

        List<RankingItem> rankedItems = new ArrayList<>();
        int rank = 1;
        for (RankingItem item : tempItems) {
            rankedItems.add(new RankingItem(
                    rank++,
                    item.stockCode(),
                    item.stockName(),
                    item.qty(),
                    item.marketValue(),
                    item.costBasis(),
                    item.pnl(),
                    item.profitRate()
            ));
        }

        return new GetRanking(member.getMemberAccountNumber(), rankedItems);
    }

    @Override
    public GetStocks getStocks(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        List<Strategy> strategies = strategyRepository.findAllByMemberAndStrategyExistedStatus(member,
                StrategyExistedStatus.EXISTED);

        Map<String, Strategy> stockMap = strategies.stream()
                .filter(s -> s.getStock() != null)
                .filter(s -> s.getStrategyProfitSummary() != null)
                .collect(Collectors.toMap(
                        s -> s.getStock().toDto().stockCode(),
                        s -> s,
                        (existing, replacement) -> existing
                ));

        BigDecimal totalMarketValue = BigDecimal.ZERO;
        for (Strategy strategy : stockMap.values()) {
            String stockCode = strategy.getStock().toDto().stockCode();
            Integer qty = transactionRepository.calculateStockQuantityByMember(member, stockCode);
            BigDecimal currentPrice = strategy.getStrategyProfitSummary().getStrategyProfitSummaryCurrentPrice();
            totalMarketValue = totalMarketValue.add(currentPrice.multiply(BigDecimal.valueOf(qty)));
        }
        totalMarketValue = totalMarketValue.setScale(2, java.math.RoundingMode.HALF_UP);

        List<StockItem> items = new ArrayList<>();
        for (Strategy strategy : stockMap.values()) {
            Stock stock = strategy.getStock();
            var stockInfo = stock.toDto();
            String stockCode = stockInfo.stockCode();

            Integer totalQty = transactionRepository.calculateStockQuantityByMember(member, stockCode);
            BigDecimal currentPrice = strategy.getStrategyProfitSummary().getStrategyProfitSummaryCurrentPrice();
            BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(totalQty))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            BigDecimal weight = totalMarketValue.compareTo(BigDecimal.ZERO) > 0
                    ? marketValue.divide(totalMarketValue, 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            StockItem item = new StockItem(
                    stockCode,
                    stockInfo.stockName(),
                    marketValue,
                    totalQty,
                    weight
            );

            items.add(item);
        }

        return new GetStocks(member.getMemberAccountNumber(), totalMarketValue, items);
    }

    @Override
    public GetStockProfit getStocksProfit(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        List<Strategy> strategies = strategyRepository.findAllByMemberAndStrategyExistedStatus(member,
                StrategyExistedStatus.EXISTED);

        Map<String, Strategy> stockMap = strategies.stream()
                .filter(s -> s.getStock() != null)
                .filter(s -> s.getStrategyProfitSummary() != null)
                .collect(Collectors.toMap(
                        s -> s.getStock().toDto().stockCode(),
                        s -> s,
                        (existing, replacement) -> existing
                ));

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<StockProfitData> stockDataList = new ArrayList<>();

        for (Strategy strategy : stockMap.values()) {
            Stock stock = strategy.getStock();
            var stockInfo = stock.toDto();
            String stockCode = stockInfo.stockCode();

            Integer totalQty = transactionRepository.calculateStockQuantityByMember(member, stockCode);
            BigDecimal netInvestment = transactionRepository.calculateNetInvestmentByMember(member, stockCode);
            BigDecimal currentPrice = strategy.getStrategyProfitSummary().getStrategyProfitSummaryCurrentPrice();
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(totalQty));

            BigDecimal profitRate = netInvestment.compareTo(BigDecimal.ZERO) > 0
                    ? totalAmount.subtract(netInvestment)
                    .divide(netInvestment, 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal avgBuyPrice = transactionRepository.calculateAverageBuyPriceByMember(member, stockCode);

            StockProfitData stockProfitData = new StockProfitData(
                    stockInfo.stockCode(),
                    stockInfo.stockName(),
                    totalQty,
                    avgBuyPrice.setScale(2, java.math.RoundingMode.HALF_UP),
                    totalAmount.setScale(2, java.math.RoundingMode.HALF_UP),
                    profitRate.setScale(2, java.math.RoundingMode.HALF_UP)
            );

            stockDataList.add(stockProfitData);
        }

        String asOf = today.format(formatter);
        return new GetStockProfit(
                member.getMemberAccountNumber(),
                asOf,
                stockDataList
        );
    }

    @Override
    public GetTransactions getTransactions(Long memberId, Pageable pageable) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        Page<Transaction> transactionPage = transactionRepository.findAllByStockOrderStrategyMemberOrderByExecutionTimeDesc(
                member, pageable);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<TransactionItem> items = transactionPage.getContent().stream()
                .map(transaction -> {
                    var stockInfo = transaction.getStock().toDto();
                    var strategy = transaction.getStockOrder().getStrategy();

                    return new TransactionItem(
                            "tr" + transaction.getId(),
                            transaction.getExecutionTime().format(formatter),
                            transaction.getTradeExecutionType().name(),
                            stockInfo.stockCode(),
                            stockInfo.stockName(),
                            new StrategyInfo(strategy.getId(), strategy.getStrategyName()),
                            transaction.getTradeExecutionQuantity(),
                            transaction.getTradeExecutionPrice().setScale(2, java.math.RoundingMode.HALF_UP)
                    );
                })
                .toList();

        PageInfo pageInfo = new PageInfo(
                transactionPage.getNumber(),
                transactionPage.getTotalPages(),
                transactionPage.getTotalElements(),
                transactionPage.getSize()
        );

        return new GetTransactions(
                member.getMemberAccountNumber(),
                items,
                pageInfo
        );
    }

    @Override
    public GetTransactionsByStock getTransactionsByStock(Long memberId, String stockCode) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        List<Transaction> transactions = transactionRepository.findAllByMemberAndStockCodeOrderByExecutionTimeDesc(
                member, stockCode);

        if (transactions.isEmpty()) {
            throw new StrategyException(ResponseMessage.STOCK_NOT_FOUND);
        }

        var stockInfo = transactions.get(0).getStock().toDto();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<TransactionByStockItem> items = transactions.stream()
                .map(transaction -> {
                    var strategy = transaction.getStockOrder().getStrategy();
                    BigDecimal price = transaction.getTradeExecutionPrice().setScale(2, java.math.RoundingMode.HALF_UP);
                    Integer qty = transaction.getTradeExecutionQuantity();
                    BigDecimal amount = price.multiply(BigDecimal.valueOf(qty))
                            .setScale(2, java.math.RoundingMode.HALF_UP);

                    return new TransactionByStockItem(
                            "tr" + transaction.getId(),
                            transaction.getExecutionTime().format(formatter),
                            transaction.getTradeExecutionType().name(),
                            new StrategyInfo(strategy.getId(), strategy.getStrategyName()),
                            qty,
                            price,
                            amount
                    );
                })
                .toList();

        return new GetTransactionsByStock(
                member.getMemberAccountNumber(),
                stockInfo.stockCode(),
                stockInfo.stockName(),
                items
        );
    }
}
