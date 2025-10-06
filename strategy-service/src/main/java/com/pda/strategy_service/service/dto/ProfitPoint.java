package com.pda.strategy_service.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProfitPoint(
        LocalDateTime date,
        BigDecimal cumulativeProfitRate
) {
}
