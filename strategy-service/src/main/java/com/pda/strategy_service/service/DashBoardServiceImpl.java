package com.pda.strategy_service.service;

import com.pda.common_service.exception.KisException;
import com.pda.common_service.exception.MemberException;
import com.pda.common_service.exception.StrategyException;
import com.pda.common_service.repository.KisTokenReader;
import com.pda.common_service.response.ResponseMessage;
import com.pda.common_service.stock.Stock;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.repository.MemberRepository;
import com.pda.strategy_service.controller.dto.DashBoardResponse.*;
import com.pda.strategy_service.controller.dto.KisPsblOrderResponse;
import com.pda.strategy_service.controller.dto.StrategyResponse.ProfitSeries;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.StrategyExistedStatus;
import com.pda.strategy_service.domain.Transaction;
import com.pda.strategy_service.repository.jpa.StrategyRepository;
import com.pda.strategy_service.repository.jpa.TransactionRepository;
import com.pda.strategy_service.service.dto.OrderPossibleBalanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashBoardServiceImpl implements DashBoardService {

    private final MemberRepository memberRepository;
    private final StrategyRepository strategyRepository;
    private final TransactionRepository transactionRepository;
    private final ProfitCalculator profitCalculator;
    private final KisTokenReader kisTokenReader;
    private final WebClient kisWebClient;

    private static final String PSBL_ORDER_PATH = "/uapi/domestic-stock/v1/trading/inquire-psbl-order";
    private static final String TR_ID_VIRTUAL_PSBL = "VTTC8908R";

    @Override
    public GetProfitRate getProfitRate(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        List<Strategy> strategies = strategyRepository.findAllByMemberAndStrategyExistedStatus(
                member, StrategyExistedStatus.EXISTED);

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

        List<Strategy> strategies = strategyRepository.findAllByMemberAndStrategyExistedStatus(
                member, StrategyExistedStatus.EXISTED);

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
                    if (totalQty == null || totalQty <= 0) return null;

                    BigDecimal netInvestment = transactionRepository.calculateNetInvestmentByMember(member, stockCode);
                    BigDecimal currentPrice = strategy.getStrategyProfitSummary().getStrategyProfitSummaryCurrentPrice();
                    BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(totalQty));
                    BigDecimal pnl = marketValue.subtract(netInvestment);
                    BigDecimal profitRate = netInvestment.compareTo(BigDecimal.ZERO) > 0
                            ? pnl.divide(netInvestment, 2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

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
                .filter(Objects::nonNull)
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

        List<Strategy> strategies = strategyRepository.findAllByMemberAndStrategyExistedStatus(
                member, StrategyExistedStatus.EXISTED);

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
            if (qty == null || qty <= 0) continue;

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
            if (totalQty == null || totalQty <= 0) continue;

            BigDecimal currentPrice = strategy.getStrategyProfitSummary().getStrategyProfitSummaryCurrentPrice();
            BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(totalQty))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal weight = totalMarketValue.compareTo(BigDecimal.ZERO) > 0
                    ? marketValue.divide(totalMarketValue, 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            items.add(new StockItem(
                    stockCode,
                    stockInfo.stockName(),
                    marketValue,
                    totalQty,
                    weight
            ));
        }

        return new GetStocks(member.getMemberAccountNumber(), totalMarketValue, items);
    }

    @Override
    public GetStockProfit getStocksProfit(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        List<Strategy> strategies = strategyRepository.findAllByMemberAndStrategyExistedStatus(
                member, StrategyExistedStatus.EXISTED);

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
            if (totalQty == null || totalQty <= 0) continue;

            BigDecimal netInvestment = transactionRepository.calculateNetInvestmentByMember(member, stockCode);
            BigDecimal currentPrice = strategy.getStrategyProfitSummary().getStrategyProfitSummaryCurrentPrice();
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(totalQty));

            BigDecimal profitRate = netInvestment.compareTo(BigDecimal.ZERO) > 0
                    ? totalAmount.subtract(netInvestment)
                    .divide(netInvestment, 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal avgBuyPrice = transactionRepository.calculateAverageBuyPriceByMember(member, stockCode);

            stockDataList.add(new StockProfitData(
                    stockInfo.stockCode(),
                    stockInfo.stockName(),
                    totalQty,
                    avgBuyPrice.setScale(2, java.math.RoundingMode.HALF_UP),
                    totalAmount.setScale(2, java.math.RoundingMode.HALF_UP),
                    profitRate.setScale(2, java.math.RoundingMode.HALF_UP)
            ));
        }

        String asOf = today.format(formatter);
        return new GetStockProfit(member.getMemberAccountNumber(), asOf, stockDataList);
    }

    @Override
    public GetTransactions getTransactions(Long memberId, Pageable pageable) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        Page<Transaction> transactionPage =
                transactionRepository.findAllByStockOrderStrategyMemberOrderByExecutionTimeDesc(member, pageable);

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

        return new GetTransactions(member.getMemberAccountNumber(), items, pageInfo);
    }

    @Override
    public GetTransactionsByStock getTransactionsByStock(Long memberId, String stockCode) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        List<Transaction> transactions = transactionRepository
                .findAllByMemberAndStockCodeOrderByExecutionTimeDesc(member, stockCode);

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

    @Override
    @Transactional(readOnly = true)
    public OrderPossibleBalanceResponse getAvailableCash(Long memberId) {
        String stockCode = "005930"; // 테스트용
        String orderPrice = "0";     // 테스트용

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        String accessToken = kisTokenReader.getMemberAccessToken(member.getId());
        String appKey = member.getMemberAppKey();
        String appSecret = member.getMemberAppSecret();
        String accountNumber = member.getMemberAccountNumber();

        try {
            String cano = accountNumber.substring(0, 8);
            String prdtCd = accountNumber.substring(8);

            log.info("[KIS 매수가능조회 요청] 계좌: {}, 종목: {}, 가격: {}", accountNumber, stockCode, orderPrice);

            KisPsblOrderResponse kisResponse = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PSBL_ORDER_PATH)
                            .queryParam("CANO", cano)
                            .queryParam("ACNT_PRDT_CD", prdtCd)
                            .queryParam("PDNO", stockCode)
                            .queryParam("ORD_UNPR", orderPrice)
                            .queryParam("ORD_DVSN", "00")
                            .queryParam("CMA_EVLU_AMT_ICLD_YN", "N")
                            .queryParam("OVRS_ICLD_YN", "N")
                            .build())
                    .header("authorization", "Bearer " + accessToken)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", TR_ID_VIRTUAL_PSBL)
                    .header("custtype", "P")
                    .retrieve()
                    .bodyToMono(KisPsblOrderResponse.class)
                    .doOnSuccess(res -> log.info("[KIS 매수가능조회 성공] 계좌: {}, 응답: {}", accountNumber, res))
                    .doOnError(WebClientResponseException.class, e ->
                            log.error("[KIS 매수가능조회 실패] HTTP 오류: {}", e.getResponseBodyAsString()))
                    .doOnError(e -> log.error("[KIS 매수가능조회 예외 발생] {}", e.getMessage()))
                    .onErrorResume(e -> {
                        throw new KisException(ResponseMessage.ORDER_CREATE_FAIL);
                    })
                    .block();

            BigDecimal orderPossibleCash = new BigDecimal(kisResponse.output().orderPossibleCash());

            return new OrderPossibleBalanceResponse(
                    member.getMemberName(),
                    accountNumber,
                    orderPossibleCash
            );
        } catch (Exception e) {
            log.error("[KIS 매수가능조회 실패] 계좌: {}, 사유: {}", member.getMemberAccountNumber(), e.getMessage());
            throw new KisException(ResponseMessage.ORDER_CREATE_FAIL);
        }
    }
}
