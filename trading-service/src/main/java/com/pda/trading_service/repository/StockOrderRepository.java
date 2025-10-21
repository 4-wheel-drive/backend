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

    @Query(value = """
                SELECT o.*
                FROM stock_order o
                JOIN strategy s ON s.id = o.strategy_id
                WHERE o.strategy_id = :strategyId
                  AND s.member_id = :memberId
            """, nativeQuery = true)
    List<StockOrder> findAllByStrategyIdAndMemberId(
            @Param("strategyId") Long strategyId,
            @Param("memberId") Long memberId
    );


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
