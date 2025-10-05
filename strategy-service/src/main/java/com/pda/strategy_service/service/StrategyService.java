package com.pda.strategy_service.service;

import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategies;

public interface StrategyService {
    ReadStrategies getStrategies(Long memberId);

//    ReadStrategy getMonoStrategy(Long strategyId, Long memberId);
}
