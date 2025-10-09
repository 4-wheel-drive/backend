package com.pda.common_service.exception;

import com.pda.common_service.response.ResponseMessage;

public class OrderException extends RuntimeException {

    private final ResponseMessage responseMessage;

    public OrderException(ResponseMessage responseMessage) {
        super(responseMessage.getMessage());
        this.responseMessage = responseMessage;
    }

    public String getCode() {
        return responseMessage.getCode();
    }
}
