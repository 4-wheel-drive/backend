package com.pda.trading_service.domain.execution;

import com.pda.common_service.BaseEntity;
import com.pda.trading_service.domain.order.StockOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeExecution extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    private TradeExecutionType tradeExecutionType;

    @NotNull
    private Integer tradeExecutionQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal tradeExecutionPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeExecutionStatus tradeExecutionStatus = TradeExecutionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private StockOrder stockOrder;
}
