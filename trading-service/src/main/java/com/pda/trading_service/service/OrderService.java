package com.pda.trading_service.service;

import com.pda.common_service.exception.MemberException;
import com.pda.common_service.exception.OrderException;
import com.pda.trading_service.event.OrderCreatedEvent;
import com.pda.trading_service.service.dto.OrderEventDto;
import com.pda.trading_service.service.kis.KisBalanceService;
import com.pda.trading_service.service.kis.dto.KisBalanceResponse;
import com.pda.trading_service.service.kis.dto.KisBalanceResponse.BalanceItem;
import com.pda.common_service.response.ResponseMessage;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.repository.MemberRepository;
import com.pda.trading_service.controller.dto.OrderReqDto.OrderCreateReqDto;
import com.pda.trading_service.domain.TradeSide;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.repository.StockOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final StockOrderRepository stockOrderRepository;
    private final MemberRepository memberRepository;
    private final KisBalanceService kisBalanceService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void createOrder(Long userId, OrderCreateReqDto dto) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        TradeSide tradeSide = TradeSide.fromString(dto.orderType());

        if (tradeSide == TradeSide.BUY) {
            validateBalance(member, dto);
        } else if (tradeSide == TradeSide.SELL) {
            validateSell(member, dto);
        }

        StockOrder order = StockOrder.createOrder(tradeSide, dto, null);

        stockOrderRepository.save(order);

        OrderEventDto orderEventDto = OrderEventDto.builder()
                .orderId(order.getId())
                .memberId(member.getId())
                .tradeSide(tradeSide)
                .stockCode(dto.stockCode())
                .quantity(dto.orderQuantity())
                .price(dto.orderPrice())
                .strategyId(dto.strategyId())
                .build();

        eventPublisher.publishEvent(new OrderCreatedEvent(this, orderEventDto));
    }

    private void validateBalance(Member member, OrderCreateReqDto orderCreateReqDto) {
        BigDecimal availableBalance = kisBalanceService.getAvailableCashSync(member, orderCreateReqDto.stockCode(),
                orderCreateReqDto.orderPrice());

        BigDecimal requiredAmount = orderCreateReqDto.orderPrice()
                .multiply(BigDecimal.valueOf(orderCreateReqDto.orderQuantity()));

        if (availableBalance.compareTo(requiredAmount) < 0) {
            throw new OrderException(ResponseMessage.DEPOSIT_DEFICIENT);
        }
    }


    private void validateSell(Member member, OrderCreateReqDto orderCreateReqDto) {
        KisBalanceResponse balance = kisBalanceService.getBalanceSync(member);
        Integer availableStocks = validateHoldingStocksQuantity(balance.balances(), orderCreateReqDto.stockCode());

        if (availableStocks < orderCreateReqDto.orderQuantity()) {
            throw new OrderException(ResponseMessage.STOCK_QUANTITY_DEFICIENT);
        }
    }

    private Integer validateHoldingStocksQuantity(List<BalanceItem> balances, String stockCode) {
        if (balances == null || balances.isEmpty()) {
            throw new OrderException(ResponseMessage.STOCK_QUANTITY_DEFICIENT);
        }

        for (BalanceItem balanceItem : balances) {
            if (stockCode.equals(balanceItem.productCode())) {
                try {
                    return Integer.parseInt(balanceItem.holdingQuantity());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        throw new OrderException(ResponseMessage.STOCK_QUANTITY_DEFICIENT);
    }
}
