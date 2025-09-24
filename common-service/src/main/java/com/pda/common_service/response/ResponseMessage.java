package com.pda.common_service.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ResponseMessage {
    /*
     * auth
     * */
    ACCESS_TOKEN_NOT_FOUND("AUTH-ACCESS-TOKEN-NOT-FOUND", "헤더에 토큰이 존재하지 않습니다."),
    TOKEN_IS_EXPIRED("TOKEN_IS_EXPIRED", "토큰이 만료되었습니다. 다시 로그인해주세요."),
    TOKEN_IS_INVALID("TOKEN_IS_INVALID", "토큰이 유효하지 않습니다.");


    private final String code;
    private final String message;
}
