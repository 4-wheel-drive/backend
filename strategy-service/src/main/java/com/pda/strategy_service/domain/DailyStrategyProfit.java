package com.pda.strategy_service.domain;

import com.pda.common_service.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyStrategyProfit extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Strategy strategy;

    @NotNull
    @Column(precision = 10, scale = 6, nullable = false)
    private BigDecimal dailyProfitRate;
}
