package com.pda.strategy_service.domain;

import com.pda.common_service.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class StrategyProfitSummary extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal strategyProfitSummaryAvgBuyPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal strategyProfitSummaryCurrentPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal strategyProfitSummaryProfitRate;

    public static StrategyProfitSummary create() {
        return StrategyProfitSummary.builder()
                .strategyProfitSummaryProfitRate(BigDecimal.ZERO)
                .strategyProfitSummaryAvgBuyPrice(BigDecimal.ZERO)
                .strategyProfitSummaryCurrentPrice(BigDecimal.ZERO).build();
    }
}
