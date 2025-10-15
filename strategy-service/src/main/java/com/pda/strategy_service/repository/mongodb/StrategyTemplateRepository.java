package com.pda.strategy_service.repository.mongodb;

import com.pda.strategy_service.domain.mongodb.StrategyTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StrategyTemplateRepository extends MongoRepository<StrategyTemplate, String> {
    
    List<StrategyTemplate> findByOwnerId(String ownerId);
    
    List<StrategyTemplate> findByStrategyName(String strategyName);
    
    Optional<StrategyTemplate> findByStrategyNameAndVersion(String strategyName, Integer version);
    
    List<StrategyTemplate> findByStrategyNameContainingIgnoreCase(String strategyName);
    
    void deleteByStrategyNameAndVersion(String strategyName, Integer version);

}
