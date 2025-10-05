package com.pda.strategy_service.controller.dto;

import com.pda.strategy_service.domain.dto.StrategyDto;
import java.math.BigDecimal;
import java.util.List;

public class StrategyResponse {
    public record ReadStrategies(
            List<StrategyDto> items
    ) {
    }

    public record ReadStrategy(
            ProfitDto strategyProfit
    ) {
    }

    public record ProfitDto(
            BigDecimal allProfit,
            BigDecimal weekProfit
    ) {
    }
}
