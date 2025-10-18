package com.pda.trading_service.controller.dto;

public record StrategyMetaDto(
        String stockId,
        String strategyName,
        String stockCode
) {
}
