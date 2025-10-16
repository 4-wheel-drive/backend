package com.pda.strategy_service.service;

import com.pda.strategy_service.controller.dto.StockResponse.ReadStocks;

public interface StockService {
    ReadStocks getAllStocks();
    ReadStocks getMyStocks(Long memberId);
}

