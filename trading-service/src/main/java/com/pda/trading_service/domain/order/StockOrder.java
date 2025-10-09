package com.pda.trading_service.domain.order;

import com.pda.common_service.BaseEntity;
import com.pda.trading_service.controller.dto.OrderReqDto.OrderCreateReqDto;
import com.pda.trading_service.domain.TradeSide;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private TradeSide tradeSide;

    @Column(nullable = false)
    private Integer orderQuantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal orderPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderKind orderKind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "strategy_id", nullable = false)
    private Long strategyId;

    // 거래 id (한투 주문번호 등)
    @Nullable
    private String tradeId;

    // === 생성 메서드 === //
    public static StockOrder createOrder(TradeSide tradeSide, OrderCreateReqDto dto, String tradeId) {
        return StockOrder.builder()
                .orderKind(OrderKind.MARKET)
                .tradeSide(tradeSide)
                .orderStatus(OrderStatus.PENDING)
                .orderQuantity(dto.orderQuantity())
                .orderPrice(dto.orderPrice())
                .strategyId(dto.strategyId())
                .tradeId(tradeId)
                .build();
    }

    public void updateStatus(OrderStatus newStatus) {
        this.orderStatus = newStatus;
    }

    public void updateExecutedPrice(BigDecimal executedPrice) {
        this.orderPrice = executedPrice;
    }

    public void updateTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public void markFailed() {
        this.orderStatus = OrderStatus.FAIL;
    }

    public void markCancelled() {
        this.orderStatus = OrderStatus.CANCELLED;
    }
}
