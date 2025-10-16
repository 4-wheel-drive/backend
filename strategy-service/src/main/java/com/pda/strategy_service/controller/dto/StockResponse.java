package com.pda.strategy_service.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class StockResponse {
    
    public record ReadStocks(
            List<StockItem> stocks
    ) {
    }
    
    public record StockItem(
            String stockName,
            String stockCode,
            @JsonProperty("image")
            String image
    ) {
    }
}

