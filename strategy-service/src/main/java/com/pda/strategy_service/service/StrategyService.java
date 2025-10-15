package com.pda.strategy_service.service;

import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategies;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategy;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.dto.StrategyMetaDto;
import com.pda.strategy_service.domain.mongodb.CustomStrategy;
import com.pda.strategy_service.domain.mongodb.StrategyTemplate;
import java.util.Map;

public interface StrategyService {
    ReadStrategies getStrategies(Long memberId);
    ReadStrategy getMonoStrategy(Long memberId, Long strategyId);
    Strategy saveStrategyMeta(Long memberId, StrategyMetaDto strategyMeta);
    CustomStrategy saveStrategy(Long strategyMetaId, Map<String, Object> strategyJson);
}
