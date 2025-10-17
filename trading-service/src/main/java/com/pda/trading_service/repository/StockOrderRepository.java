package com.pda.trading_service.repository;

import com.pda.trading_service.domain.order.OrderStatus;
import com.pda.trading_service.domain.order.StockOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockOrderRepository extends JpaRepository<StockOrder, Long> {
    List<StockOrder> findAllByStrategyId(Long strategyId);

    Optional<StockOrder> findByTradeId(String tradeId);

    @Query("SELECT o FROM StockOrder o " +
            "WHERE o.orderStatus = :status " +
            "AND DATE(o.createdAt) = CURRENT_DATE")
    List<StockOrder> findByStatusAndCreateAtToday(@Param("status") OrderStatus status);
}
