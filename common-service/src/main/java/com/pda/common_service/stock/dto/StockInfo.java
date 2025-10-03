package com.pda.common_service.stock.dto;

public record StockInfo(
        String stockCode,
        String stockImgUri,
        String stockName
) {
}
