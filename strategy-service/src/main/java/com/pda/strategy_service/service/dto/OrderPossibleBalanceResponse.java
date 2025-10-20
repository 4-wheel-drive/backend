package com.pda.strategy_service.service.dto;

import java.math.BigDecimal;

public record OrderPossibleBalanceResponse(
        String memberName,
        String accountNumber,
        BigDecimal orderPossibleCash
) {
}