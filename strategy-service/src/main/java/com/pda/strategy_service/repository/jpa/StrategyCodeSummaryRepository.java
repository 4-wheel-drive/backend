package com.pda.strategy_service.repository.jpa;

import com.pda.strategy_service.domain.StrategyCodeSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyCodeSummaryRepository extends JpaRepository<StrategyCodeSummary, Long> {
}
