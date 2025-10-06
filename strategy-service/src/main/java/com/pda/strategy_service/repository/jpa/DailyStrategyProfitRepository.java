package com.pda.strategy_service.repository.jpa;

import com.pda.strategy_service.domain.DailyStrategyProfit;
import com.pda.strategy_service.domain.Strategy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyStrategyProfitRepository extends JpaRepository<DailyStrategyProfit, Long> {
    List<DailyStrategyProfit> findAllByStrategy(Strategy strategy);
}
