package com.pda.trading_service.controller.dto;

import java.math.BigDecimal;

public class OrderReqDto {
    public record OrderCreateReqDto(
            Long strategyId,
            Integer orderQuantity,
            BigDecimal orderPrice,
            String stockCode,
            String orderType) {
    }
}
