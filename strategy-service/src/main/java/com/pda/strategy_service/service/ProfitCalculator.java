package com.pda.strategy_service.service;

import com.pda.strategy_service.domain.DailyStrategyProfit;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.repository.DailyStrategyProfitRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfitCalculator {
    private final DailyStrategyProfitRepository dailyStrategyProfitRepository;

    public BigDecimal allCumulativeProfit(Strategy strategy) {
        List<DailyStrategyProfit> profits = dailyStrategyProfitRepository.findAllByStrategy(strategy);

        if (profits.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal cumulative = BigDecimal.ONE;
        for (DailyStrategyProfit profit : profits) {
            cumulative = cumulative.multiply(BigDecimal.ONE.add(profit.getDailyProfitRate()));
        }

        return cumulative.subtract(BigDecimal.ONE)
                .setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal weekCumulativeProfit(Strategy strategy) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<DailyStrategyProfit> profits = dailyStrategyProfitRepository.findAllByStrategy(strategy)
                .stream()
                .filter(p -> p.getCreatedAt().isAfter(sevenDaysAgo))
                .sorted(Comparator.comparing(DailyStrategyProfit::getCreatedAt))
                .toList();

        if (profits.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal cumulative = BigDecimal.ONE;
        for (DailyStrategyProfit profit : profits) {
            cumulative = cumulative.multiply(BigDecimal.ONE.add(profit.getDailyProfitRate()));
        }

        return cumulative.subtract(BigDecimal.ONE)
                .setScale(4, RoundingMode.HALF_UP);
    }
}
