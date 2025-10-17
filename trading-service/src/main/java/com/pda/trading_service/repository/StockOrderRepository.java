package com.pda.trading_service.repository;

import com.pda.trading_service.domain.order.OrderStatus;
import com.pda.trading_service.domain.order.StockOrder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockOrderRepository extends JpaRepository<StockOrder, Long> {
    List<StockOrder> findAllByStrategyId(Long strategyId);

    Optional<StockOrder> findByTradeId(String tradeId);

    @Query(value = """
                SELECT *
                FROM stock_order
                WHERE order_status = :status
                AND created_at BETWEEN :startOfDay AND :endOfDay
            """, nativeQuery = true)
    List<StockOrder> findByStatusAndCreateAtToday(
            @Param("status") String status,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
