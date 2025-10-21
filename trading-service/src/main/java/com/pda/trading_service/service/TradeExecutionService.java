package com.pda.trading_service.service;

import com.pda.trading_service.controller.dto.TradeExecutionResponseDto;
import org.springframework.data.domain.Pageable;

public interface TradeExecutionService {
    TradeExecutionResponseDto.ReadTradeExecution getTradeExecutions(Long memberId, Long strategyId, Pageable pageable);
}
