package com.pda.strategy_service.repository.jpa;

import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.StrategySummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategySummaryRepository extends JpaRepository<StrategySummary, Long> {
    StrategySummary findByStrategy(Strategy strategy);
}