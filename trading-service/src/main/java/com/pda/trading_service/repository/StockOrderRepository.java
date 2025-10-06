package com.pda.trading_service.repository;

import com.pda.trading_service.domain.order.StockOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockOrderRepository extends JpaRepository<StockOrder, Long> {
    List<StockOrder> findAllByStrategyId(Long strategyId);
}

