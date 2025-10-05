package com.pda.strategy_service.controller.dto;

import com.pda.strategy_service.domain.dto.StrategyDto;
import com.pda.strategy_service.service.dto.ProfitPoint;
import java.math.BigDecimal;
import java.util.List;

public class StrategyResponse {
    public record ReadStrategies(
            List<StrategyDto> items
    ) {
    }

    public record ReadStrategy(
            ProfitDto strategyProfit,
            ProfitSeries profitSeries
    ) {
    }

    public record ProfitDto(
            BigDecimal allProfit,
            BigDecimal weekProfit
    ) {
    }

    public record ProfitSeries(
            List<ProfitPoint> oneMonth,
            List<ProfitPoint> threeMonth,
            List<ProfitPoint> sixMonth,
            List<ProfitPoint> oneYear,
            List<ProfitPoint> all
    ) {
    }
}
