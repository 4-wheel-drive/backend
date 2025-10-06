package com.pda.common_service.exception;

import com.pda.common_service.response.ResponseMessage;

public class ResourceNotFound extends RuntimeException {
    private final ResponseMessage responseMessage;

    public ResourceNotFound(ResponseMessage responseMessage) {
        super(responseMessage.getMessage());
        this.responseMessage = responseMessage;
    }

    public String getCode() {
        return responseMessage.getCode();
    }
}
