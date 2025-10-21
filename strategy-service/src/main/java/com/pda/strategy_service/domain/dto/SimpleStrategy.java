package com.pda.strategy_service.domain.dto;

import com.pda.strategy_service.domain.StrategyActivatedStatus;

public record SimpleStrategy(Long id, String strategyName, StrategyActivatedStatus strategyActivatedStatus) {
}
