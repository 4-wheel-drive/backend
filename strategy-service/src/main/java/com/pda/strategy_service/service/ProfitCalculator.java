package com.pda.strategy_service.service;

import com.pda.strategy_service.controller.dto.StrategyResponse.ProfitSeries;
import com.pda.strategy_service.domain.DailyStrategyProfit;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.repository.DailyStrategyProfitRepository;
import com.pda.strategy_service.service.dto.ProfitPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfitCalculator {
    private final DailyStrategyProfitRepository dailyStrategyProfitRepository;

    /**
     * 전체 누적 수익률 계산
     */
    public BigDecimal allCumulativeProfit(Strategy strategy) {
        return calculateCumulative(strategy, null);
    }

    /**
     * 최근 7일 누적 수익률 계산
     */
    public BigDecimal weekCumulativeProfit(Strategy strategy) {
        return calculateCumulative(strategy, LocalDateTime.now().minusDays(7));
    }

    /**
     * 특정 기간(개월 단위) 누적 수익률 계산 (공통)
     */
    private BigDecimal calculateCumulative(Strategy strategy, LocalDateTime fromDate) {
        List<DailyStrategyProfit> profits = dailyStrategyProfitRepository.findAllByStrategy(strategy)
                .stream()
                .filter(p -> fromDate == null || p.getCreatedAt().isAfter(fromDate))
                .sorted(Comparator.comparing(DailyStrategyProfit::getCreatedAt))
                .toList();

        if (profits.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal cumulative = BigDecimal.ONE;
        for (DailyStrategyProfit profit : profits) {
            cumulative = cumulative.multiply(BigDecimal.ONE.add(profit.getDailyProfitRate()));
        }

        return cumulative.subtract(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP);
    }

    public ProfitSeries getAllPeriodSeries(Strategy strategy) {
        return new ProfitSeries(
                cumulativeTimeSeries(strategy, LocalDateTime.now().minusMonths(1)),
                cumulativeTimeSeries(strategy, LocalDateTime.now().minusMonths(3)),
                cumulativeTimeSeries(strategy, LocalDateTime.now().minusMonths(6)),
                cumulativeTimeSeries(strategy, LocalDateTime.now().minusYears(1)),
                cumulativeTimeSeries(strategy, null)
        );
    }

    private List<ProfitPoint> cumulativeTimeSeries(Strategy strategy, LocalDateTime fromDate) {
        List<DailyStrategyProfit> profits = dailyStrategyProfitRepository.findAllByStrategy(strategy)
                .stream()
                .filter(p -> fromDate == null || p.getCreatedAt().isAfter(fromDate))
                .sorted(Comparator.comparing(DailyStrategyProfit::getCreatedAt))
                .toList();

        List<ProfitPoint> result = new ArrayList<>();
        if (profits.isEmpty()) {
            return result;
        }

        BigDecimal cumulative = BigDecimal.ONE;
        for (DailyStrategyProfit profit : profits) {
            cumulative = cumulative.multiply(BigDecimal.ONE.add(profit.getDailyProfitRate()));
            BigDecimal cumulativeRate = cumulative.subtract(BigDecimal.ONE)
                    .setScale(4, RoundingMode.HALF_UP);
            result.add(new ProfitPoint(profit.getCreatedAt(), cumulativeRate));
        }

        return result;
    }
}
