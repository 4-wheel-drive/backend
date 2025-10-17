package com.pda.trading_service.repository;

import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.order.StockOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeExecutionRepository extends JpaRepository<TradeExecution, Long> {
    List<TradeExecution> findAllByStockOrder(StockOrder stockOrder);

    Optional<StockOrder> findByStockOrder(StockOrder stockOrder);
}
