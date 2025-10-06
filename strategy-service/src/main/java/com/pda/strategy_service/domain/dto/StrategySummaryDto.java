package com.pda.strategy_service.domain.dto;

public record StrategySummaryDto(
        String summaryOverview,
        String summaryCondition,
        String summaryRisk
) {
}
