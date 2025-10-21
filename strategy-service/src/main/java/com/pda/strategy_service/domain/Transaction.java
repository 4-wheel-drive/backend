package com.pda.strategy_service.domain;

import com.pda.common_service.BaseEntity;
import com.pda.common_service.stock.Stock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trade_execution")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_order_id", unique = true, nullable = false)
    private StockOrder stockOrder;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "trade_execution_type", nullable = false)
    private TransactionSide tradeExecutionType;

    @Column(name = "trade_execution_quantity", nullable = false)
    private Integer tradeExecutionQuantity;

    @Column(name = "trade_execution_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal tradeExecutionPrice;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "trade_execution_status", nullable = false)
    private TransactionStatus tradeExecutionStatus;

    @Column(name = "execution_time")
    private LocalDateTime executionTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code")
    private Stock stock;
}
