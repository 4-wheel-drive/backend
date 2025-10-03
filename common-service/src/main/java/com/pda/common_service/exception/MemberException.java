package com.pda.common_service.exception;

import com.pda.common_service.response.ResponseMessage;
import lombok.Getter;

@Getter
public class MemberException extends RuntimeException {

    private final ResponseMessage responseMessage;

    public MemberException(ResponseMessage responseMessage) {
        super(responseMessage.getMessage());
        this.responseMessage = responseMessage;
    }

    public String getCode() {
        return responseMessage.getCode();
    }
}
