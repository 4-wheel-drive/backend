package com.pda.trading_service.domain;

import com.pda.common_service.exception.OrderException;
import com.pda.common_service.response.ResponseMessage;

public enum TradeSide {
    BUY,
    SELL;

    public static TradeSide fromString(String tradeSide) {
        if (tradeSide == null) {
            throw new OrderException(ResponseMessage.ORDER_CREATE_FAIL);
        }

        for (TradeSide ot : TradeSide.values()) {
            if (ot.name().equalsIgnoreCase(tradeSide)) {
                return ot;
            }
        }

        throw new OrderException(ResponseMessage.ORDER_CREATE_FAIL);
    }
}