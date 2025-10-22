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
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfitCalculator {
    private final DailyStrategyProfitRepository dailyStrategyProfitRepository;

    /**
     * 전체 누적 수익률 계산 (% 단위)
     */
    public BigDecimal allCumulativeProfit(Strategy strategy) {
        return calculateCumulative(strategy, null);
    }

    /**
     * 최근 7일 누적 수익률 계산 (% 단위)
     */
    public BigDecimal weekCumulativeProfit(Strategy strategy) {
        return calculateCumulative(strategy, LocalDateTime.now().minusDays(7));
    }

    /**
     * 특정 기간(개월 단위) 누적 수익률 계산 (공통, % 단위)
     */
    public BigDecimal calculateCumulative(Strategy strategy, LocalDateTime fromDate) {
        List<DailyStrategyProfit> profits = dailyStrategyProfitRepository.findAllByStrategy(strategy)
                .stream()
                .filter(p -> fromDate == null || p.getCreatedAt().isAfter(fromDate))
                .sorted(Comparator.comparing(DailyStrategyProfit::getCreatedAt))
                .toList();

        if (profits.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal cumulative = BigDecimal.ONE;
        for (DailyStrategyProfit profit : profits) {
            cumulative = cumulative.multiply(BigDecimal.ONE.add(profit.getDailyProfitRate()));
        }

        // % 단위 변환 (예: 0.12 → 12.00)
        return cumulative.subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 전략별 전체 기간별 시리즈 (1개월, 3개월, 6개월, 1년, 전체)
     */
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

    /**
     * 단일 전략의 누적 수익률 시리즈 (% 단위)
     */
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
                    .multiply(BigDecimal.valueOf(100)) // % 변환
                    .setScale(2, RoundingMode.HALF_UP);
            result.add(new ProfitPoint(profit.getCreatedAt(), cumulativeRate));
        }

        return result;
    }

    /**
     * 여러 전략의 날짜별 평균 수익률을 기반으로 계좌 전체 누적 수익률 계산 (% 단위)
     */
    private List<ProfitPoint> cumulativeTimeSeriesForAccount(List<Strategy> strategies, LocalDateTime fromDate) {
        // 모든 전략의 DailyStrategyProfit을 날짜별로 그룹화
        Map<LocalDateTime, ArrayList<DailyStrategyProfit>> profitsByDate = new HashMap<>();

        for (Strategy strategy : strategies) {
            List<DailyStrategyProfit> profits = dailyStrategyProfitRepository.findAllByStrategy(strategy);
            for (DailyStrategyProfit profit : profits) {
                profitsByDate
                        .computeIfAbsent(profit.getCreatedAt(), k -> new ArrayList<>())
                        .add(profit);
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
                    .multiply(BigDecimal.valueOf(100)) // % 변환
                    .setScale(2, RoundingMode.HALF_UP);

            result.add(new ProfitPoint(date, cumulativeRate));
        }

        return result;
    }
}
