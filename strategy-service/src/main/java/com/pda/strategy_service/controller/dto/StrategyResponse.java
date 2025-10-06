package com.pda.strategy_service.controller.dto;

import com.pda.common_service.stock.dto.StockInfo;
import com.pda.strategy_service.domain.dto.SimpleStrategy;
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
            StockInfo stockInfo,
            SimpleStrategy strategyInfo,
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
