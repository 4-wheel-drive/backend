package com.pda.trading_service.domain.execution;

import com.pda.common_service.BaseEntity;
import com.pda.trading_service.domain.TradeSide;
import com.pda.trading_service.domain.execution.dto.TradeExecutionDto;
import com.pda.trading_service.domain.order.StockOrder;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeExecution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_order_id", nullable = false)
    private StockOrder stockOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_execution_type", nullable = false)
    private TradeSide tradeSide;

    @Column(name = "trade_execution_quantity", nullable = false)
    private Integer quantity;

    @Column(name = "trade_execution_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_execution_status", nullable = false)
    private TradeExecutionStatus status;

    @Column(name = "execution_time")
    private LocalDateTime executionTime;

    @Column(length = 30)
    private String code;

    public void updateStatus(TradeExecutionStatus newStatus) {
        this.status = newStatus;
    }

    public TradeExecutionDto toDto() {
        return new TradeExecutionDto(
                this.id,
                this.tradeSide,
                this.quantity,
                this.price,
                this.executionTime
        );
    }
}
