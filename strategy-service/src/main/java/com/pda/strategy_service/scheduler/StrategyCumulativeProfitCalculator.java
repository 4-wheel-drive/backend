package com.pda.strategy_service.scheduler;


import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.StrategyProfitSummary;
import com.pda.strategy_service.domain.Transaction;
import com.pda.strategy_service.repository.jpa.StrategyRepository;
import com.pda.strategy_service.repository.jpa.StrategyProfitSummaryRepository;
import com.pda.strategy_service.repository.jpa.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyCumulativeProfitCalculator {

    private final StrategyRepository strategyRepository;
    private final TransactionRepository transactionRepository;
    private final StrategyProfitSummaryRepository strategyProfitSummaryRepository;

    /**
     * 체결 이벤트 기반 — 단일 전략의 누적 수익률 갱신
     */
    @Transactional
    public void updateCumulativeProfit(Long strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 전략이 존재하지 않습니다: " + strategyId));

        List<Transaction> transactions = transactionRepository.findAllByStockOrderStrategy(strategyId);
        if (transactions.isEmpty()) {
            log.warn("[SKIP] 전략 {} 거래내역 없음", strategyId);
            return;
        }

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalReturned = BigDecimal.ZERO;
        int totalQty = 0;

        for (Transaction tx : transactions) {
            BigDecimal price = tx.getTradeExecutionPrice();
            int qty = tx.getTradeExecutionQuantity();

            switch (tx.getTradeExecutionType()) {
                case BUY -> {
                    totalInvested = totalInvested.add(price.multiply(BigDecimal.valueOf(qty)));
                    totalQty += qty;
                }
                case SELL -> {
                    totalReturned = totalReturned.add(price.multiply(BigDecimal.valueOf(qty)));
                    totalQty -= qty;
                }
            }
        }

        // 평가금액 = 현재가 * 보유수량
        BigDecimal currentPrice = strategy.getStrategyProfitSummary().getStrategyProfitSummaryCurrentPrice();
        BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(totalQty));
        BigDecimal totalPnL = totalReturned.add(marketValue).subtract(totalInvested);

        // 누적 수익률 계산
        BigDecimal cumulativeReturn = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? totalPnL.divide(totalInvested, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Summary 갱신 (변경감지)
        StrategyProfitSummary summary = strategy.getStrategyProfitSummary();
        summary.updateProfitSummary(
                summary.getStrategyProfitSummaryAvgBuyPrice(),
                summary.getStrategyProfitSummaryCurrentPrice(),
                cumulativeReturn.setScale(2, RoundingMode.HALF_UP)
        );

        strategyProfitSummaryRepository.save(summary);

        log.info("[Event] 전략 {} 누적 수익률 업데이트 완료 | 수익률: {}% | 손익: {}",
                strategyId, cumulativeReturn, totalPnL);
    }
}
