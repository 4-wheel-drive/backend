package com.pda.trading_service.repository;

import com.pda.trading_service.domain.order.StockOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockOrderRepository extends JpaRepository<StockOrder, Long> {
    @Query(value = "SELECT * FROM stock_order WHERE strategy_id = :strategyId", nativeQuery = true)
    List<StockOrder> findAllByStrategyId(@Param("strategyId") Long strategyId);
}

