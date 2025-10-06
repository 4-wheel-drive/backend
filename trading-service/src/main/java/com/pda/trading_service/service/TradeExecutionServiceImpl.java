package com.pda.trading_service.service;

import com.pda.trading_service.controller.dto.TradeExecutionResponseDto;
import com.pda.trading_service.controller.dto.TradeExecutionResponseDto.ReadTradeExecution;
import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.execution.dto.TradeExecutionDto;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.repository.StockOrderRepository;
import com.pda.trading_service.repository.TradeExecutionRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeExecutionServiceImpl implements TradeExecutionService {
    private final StockOrderRepository stockOrderRepository;
    private final TradeExecutionRepository tradeExecutionRepository;

    @Override
    public ReadTradeExecution getTradeExecution(Long memberId, Long strategyId) {
        List<StockOrder> stockOrders = stockOrderRepository.findAllByStrategyId(strategyId);
        List<TradeExecutionDto> tradeExecutionList = new ArrayList<>();

        for (StockOrder stockOrder : stockOrders) {
            List<TradeExecution> tradeExecutions = tradeExecutionRepository.findAllByStockOrder(stockOrder);

            for (TradeExecution execution : tradeExecutions) {
                TradeExecutionDto tradeExecutionDto = execution.toDto();
                tradeExecutionList.add(tradeExecutionDto);
            }
        }
        tradeExecutionList.sort((a, b) -> b.executionTime().compareTo(a.executionTime()));
        return new TradeExecutionResponseDto.ReadTradeExecution(tradeExecutionList.size(), tradeExecutionList);
    }
}
