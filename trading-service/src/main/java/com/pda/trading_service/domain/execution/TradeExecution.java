package com.pda.trading_service.domain.execution;

import com.pda.common_service.BaseEntity;
import com.pda.common_service.stock.Stock;
import com.pda.trading_service.domain.TradeSide;
import com.pda.trading_service.domain.execution.dto.TradeExecutionDto;
import com.pda.trading_service.domain.order.StockOrder;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeExecution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주문 참조 (FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_order_id", nullable = false)
    private StockOrder stockOrder;

    /** 매수/매도 구분 */
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_execution_type", nullable = false)
    private TradeSide tradeSide;

    /** 체결 수량 */
    @Column(name = "trade_execution_quantity", nullable = false)
    private Integer quantity;

    /** 평균 체결 단가 */
    @Column(name = "trade_execution_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal price;

    /** 체결 상태 (FILLED, PARTIALLY_FILLED, PENDING 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_execution_status", nullable = false)
    private TradeExecutionStatus status;

    /** 체결 시각 */
    @Column(name = "execution_time")
    private LocalDateTime executionTime;

    /** 종목 코드 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code")
    private Stock stock;

    /** 상태 업데이트 */
    public void updateStatus(TradeExecutionStatus newStatus) {
        this.status = newStatus;
    }

    public static TradeExecution create(StockOrder stockOrder,
                                        TradeExecutionStatus tradeExecutionStatus,
                                        int filledQuantity,
                                        double avgPrice, Stock stock) {
        return TradeExecution.builder()
                .stockOrder(stockOrder)
                .tradeSide(stockOrder.getTradeSide())
                .status(tradeExecutionStatus)
                .quantity(filledQuantity)
                .price(BigDecimal.valueOf(avgPrice))
                .executionTime(LocalDateTime.now())
                .stock(stock)
                .build();
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
