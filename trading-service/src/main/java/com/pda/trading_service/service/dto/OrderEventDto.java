package com.pda.trading_service.service.dto;

import com.pda.trading_service.domain.TradeSide;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderEventDto(
        Long orderId,
        Long memberId,
        String stockCode,
        TradeSide tradeSide,
        int quantity,
        BigDecimal price,
        Long strategyId
) {}
