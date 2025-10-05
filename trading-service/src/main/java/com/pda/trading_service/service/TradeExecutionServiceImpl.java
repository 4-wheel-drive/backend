package com.pda.trading_service.service;

import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.execution.TradeExecutionStatus;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.repository.StockOrderRepository;
import com.pda.trading_service.repository.TradeExecutionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeExecutionServiceImpl implements TradeExecutionService {
    private final StockOrderRepository stockOrderRepository;
    private final TradeExecutionRepository tradeExecutionRepository;

    @Override
    public void getTradeExecution(Long memberId, Long strategyId) {
        List<StockOrder> stockOrders = stockOrderRepository.findAllByStrategyId(strategyId);
        // 체결 시간, 매매, 수량, 금액,

        for (StockOrder stockOrder : stockOrders) {
            List<TradeExecution> tradeExecutions = tradeExecutionRepository.findAllByStockOrder(stockOrder);

            for (TradeExecution execution : tradeExecutions) {
                System.out.printf(
                        " - 체결 [%s] 수량: %d, 가격: %.2f, 상태: %s%n",
                        execution.getTradeExecutionType(),
                        execution.getTradeExecutionQuantity(),
                        execution.getTradeExecutionPrice(),
                        execution.getTradeExecutionStatus()
                );
            }
        }
    }
}
