package com.pda.common_service.exception;

import com.pda.common_service.response.ResponseMessage;
import lombok.Getter;

@Getter
public class StrategyTemplatesException extends RuntimeException {

    private final ResponseMessage responseMessage;

    public StrategyTemplatesException(ResponseMessage responseMessage) {
        super(responseMessage.getMessage());
        this.responseMessage = responseMessage;
    }

    public String getCode() {
        return responseMessage.getCode();
    }
}
