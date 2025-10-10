package com.pda.strategy_service.service;

import com.pda.strategy_service.controller.dto.StrategyResponse.ProfitSeries;
import com.pda.strategy_service.domain.DailyStrategyProfit;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.repository.jpa.DailyStrategyProfitRepository;
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
    public BigDecimal calculateCumulative(Strategy strategy, LocalDateTime fromDate) {
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

    /**
     * 여러 전략의 전체 계좌 수익률 계산 (기간별)
     */
    public ProfitSeries getAllPeriodSeriesForAccount(List<Strategy> strategies) {
        List<ProfitPoint> allData = cumulativeTimeSeriesForAccount(strategies, null);
        
        LocalDateTime now = LocalDateTime.now();
        
        return new ProfitSeries(
                filterByDate(allData, now.minusMonths(1)),
                filterByDate(allData, now.minusMonths(3)),
                filterByDate(allData, now.minusMonths(6)),
                filterByDate(allData, now.minusYears(1)),
                allData
        );
    }
    
    /**
     * 특정 날짜 이후의 데이터만 필터링
     */
    private List<ProfitPoint> filterByDate(List<ProfitPoint> allData, LocalDateTime fromDate) {
        return allData.stream()
                .filter(point -> point.date().isAfter(fromDate))
                .toList();
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

    /**
     * 여러 전략의 날짜별 평균 수익률을 기반으로 계좌 전체 누적 수익률 계산
     */
    private List<ProfitPoint> cumulativeTimeSeriesForAccount(List<Strategy> strategies, LocalDateTime fromDate) {
        // 모든 전략의 DailyStrategyProfit을 날짜별로 그룹화
        var profitsByDate = new java.util.HashMap<LocalDateTime, java.util.ArrayList<DailyStrategyProfit>>();
        
        for (Strategy strategy : strategies) {
            List<DailyStrategyProfit> profits = dailyStrategyProfitRepository.findAllByStrategy(strategy);
            for (DailyStrategyProfit profit : profits) {
                profitsByDate.computeIfAbsent(profit.getCreatedAt(), k -> new java.util.ArrayList<>()).add(profit);
            }
        }

        // 날짜 필터링 및 정렬
        List<LocalDateTime> sortedDates = profitsByDate.keySet().stream()
                .filter(date -> fromDate == null || date.isAfter(fromDate))
                .sorted()
                .toList();

        List<ProfitPoint> result = new ArrayList<>();
        if (sortedDates.isEmpty()) {
            return result;
        }

        BigDecimal cumulative = BigDecimal.ONE;

        for (LocalDateTime date : sortedDates) {
            List<DailyStrategyProfit> dailyProfits = profitsByDate.get(date);

            // 해당 날짜의 평균 수익률 계산
            BigDecimal avgDailyRate = dailyProfits.stream()
                    .map(DailyStrategyProfit::getDailyProfitRate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(dailyProfits.size()), 4, RoundingMode.HALF_UP);

            cumulative = cumulative.multiply(BigDecimal.ONE.add(avgDailyRate));
            BigDecimal cumulativeRate = cumulative.subtract(BigDecimal.ONE)
                    .setScale(4, RoundingMode.HALF_UP);

            result.add(new ProfitPoint(date, cumulativeRate));
        }

        return result;
    }
}
