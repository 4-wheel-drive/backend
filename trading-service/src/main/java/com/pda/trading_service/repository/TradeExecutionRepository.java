package com.pda.trading_service.repository;

import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.order.StockOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeExecutionRepository extends JpaRepository<TradeExecution, Long> {
    List<TradeExecution> findAllByStockOrder(StockOrder stockOrder);

    Optional<TradeExecution> findByStockOrder(StockOrder stockOrder);

    @Query("""
                SELECT e
                FROM TradeExecution e
                JOIN FETCH e.stockOrder o
                LEFT JOIN FETCH e.stock s
                WHERE o.id IN :stockOrderIds
                ORDER BY e.executionTime DESC
            """)
    Page<TradeExecution> findAllByStockOrderIdInWithOrder(
            @Param("stockOrderIds") List<Long> stockOrderIds,
            Pageable pageable
    );

    boolean existsByStockOrder(StockOrder stockOrder);
}
