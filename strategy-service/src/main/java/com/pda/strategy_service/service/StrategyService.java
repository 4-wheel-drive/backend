package com.pda.strategy_service.service;

import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategies;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategy;

public interface StrategyService {
    ReadStrategies getStrategies(Long memberId);
    ReadStrategy getMonoStrategy(Long strategyId);
}
