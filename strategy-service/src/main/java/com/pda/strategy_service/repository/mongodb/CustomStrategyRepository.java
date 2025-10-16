package com.pda.strategy_service.repository.mongodb;

import com.pda.strategy_service.domain.mongodb.CustomStrategy;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomStrategyRepository extends MongoRepository<CustomStrategy, String> {
    CustomStrategy findByStrategyId(Long strategyId);
    void deleteByStrategyId(Long strategyId);
}
