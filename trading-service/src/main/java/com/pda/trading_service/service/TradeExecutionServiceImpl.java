package com.pda.trading_service.service;

import com.pda.trading_service.controller.dto.TradeExecutionResponseDto;
import com.pda.trading_service.controller.dto.TradeExecutionResponseDto.ReadTradeExecution;
import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.execution.dto.TradeExecutionDto;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.repository.StockOrderRepository;
import com.pda.trading_service.repository.TradeExecutionRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeExecutionServiceImpl implements TradeExecutionService {

    private final StockOrderRepository stockOrderRepository;
    private final TradeExecutionRepository tradeExecutionRepository;

    @Override
    @Transactional
    public ReadTradeExecution getTradeExecutions(Long memberId, Long strategyId, Pageable pageable) {
        List<StockOrder> stockOrders = stockOrderRepository.findAllByStrategyIdAndMemberId(strategyId, memberId);

        if (stockOrders.isEmpty()) {
            return ReadTradeExecution.of(
                    List.of(), 0, 0L, pageable.getPageNumber(), pageable.getPageSize()
            );
        }
        List<Long> stockOrderIds = stockOrders.stream()
                .map(StockOrder::getId)
                .toList();
        Page<TradeExecution> executionPage =
                tradeExecutionRepository.findAllByStockOrderIdInWithOrder(stockOrderIds, pageable);
        List<TradeExecutionDto> tradeExecutionDtos = executionPage.getContent().stream()
                .map(TradeExecution::toDto)
                .toList();
        return TradeExecutionResponseDto.ReadTradeExecution.of(
                tradeExecutionDtos,
                executionPage.getTotalPages(),
                executionPage.getTotalElements(),
                executionPage.getNumber(),
                executionPage.getSize()
        );
    }
}
