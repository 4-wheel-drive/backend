package com.pda.strategy_service.service;

import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategies;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategy;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.dto.StrategyMetaDto;

public interface StrategyService {
    ReadStrategies getStrategies(Long memberId);
    ReadStrategy getMonoStrategy(Long strategyId);
    Strategy saveStrategy(Long memberId, StrategyMetaDto strategyMeta);
}
