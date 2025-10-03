package com.pda.common_service.exception;

import com.pda.common_service.response.ResponseMessage;

public class StrategyException extends RuntimeException {

    private final ResponseMessage responseMessage;

    public StrategyException(ResponseMessage responseMessage) {
        super(responseMessage.getMessage());
        this.responseMessage = responseMessage;
    }

    public String getCode() {
        return responseMessage.getCode();
    }
}