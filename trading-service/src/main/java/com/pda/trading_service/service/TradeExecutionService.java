package com.pda.trading_service.service;

import com.pda.trading_service.controller.dto.TradeExecutionResponseDto.ReadTradeExecution;

public interface TradeExecutionService {
    ReadTradeExecution getTradeExecution(Long memberId, Long strategyId);
}
