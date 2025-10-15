package com.pda.strategy_service.service.mongodb;

import com.pda.strategy_service.domain.mongodb.StrategyTemplate;

import java.util.List;
import java.util.Map;

public interface StrategyTemplateService {

    StrategyTemplate saveStrategyTemplate(Map<String, Object> strategyJson);

    List<StrategyTemplate> getAllStrategyTemplates();
    
//    List<StrategyTemplate> getStrategyTemplatesByOwner(String ownerId);
    
    StrategyTemplate getStrategyTemplateByNameAndVersion(String strategyName, Integer version);
    
    StrategyTemplate getStrategyTemplateById(String strategyId);
    
    List<StrategyTemplate> searchStrategyTemplatesByName(String strategyName);
    
    void deleteStrategyTemplate(String strategyName, Integer version);

    void initializeDefaultStrategyTemplates();

    void forceReinitializeStrategyTemplates();

    void updateStrategyTemplates();
}
