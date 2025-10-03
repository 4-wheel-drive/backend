package com.pda.strategy_service.domain.dto;

import com.pda.common_service.stock.dto.StockInfo;
import com.pda.strategy_service.domain.StrategyActivatedStatus;
import java.math.BigDecimal;

public record StrategyDto(
        Long id,
        StockInfo stockInfo,
        String strategyName,
        StrategyActivatedStatus activatedStatus,
        BigDecimal profitRate,
        BigDecimal profitAmount,
        BigDecimal avgPrice,
        BigDecimal currentPrice
) {
}
