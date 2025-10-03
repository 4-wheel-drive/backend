package com.pda.strategy_service.domain;

import com.pda.common_service.stock.MemberStock;
import com.pda.common_service.stock.dto.StockInfo;
import com.pda.strategy_service.domain.dto.StrategyDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Strategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private MemberStock memberStock;

    @OneToOne
    private StrategyProfitSummary strategyProfitSummary;

    @Column(length = 100)
    private String strategyName;

    @Enumerated(value = EnumType.STRING)
    private StrategyActivatedStatus strategyIsActivated;

    @Enumerated(value = EnumType.STRING)
    private StrategyExistedStatus strategyIsDeleted;

    public StrategyDto toDto(StockInfo stockInfo, BigDecimal profitAmount) {
        return new StrategyDto(
                id,
                stockInfo,
                strategyName,
                strategyIsActivated,
                strategyProfitSummary.getProfitRate(),
                profitAmount,
                strategyProfitSummary.getAvgBuyPrice(),
                strategyProfitSummary.getCurrentPrice()
        );
    }
}
