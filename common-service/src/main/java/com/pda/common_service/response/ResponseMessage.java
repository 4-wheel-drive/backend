package com.pda.common_service.response;

import com.pda.common_service.exception.AuthException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ResponseMessage {

    /*
     * member
     * */
    MEMBER_NOT_FOUND("MEMBER-NOT-FOUND", "존재하지 않는 유저입니다"),

    /*
     * auth
     * */
    ACCESS_TOKEN_NOT_FOUND("AUTH-ACCESS-TOKEN-NOT-FOUND", "헤더에 토큰이 존재하지 않습니다."),
    TOKEN_IS_EXPIRED("TOKEN-IS-EXPIRED", "토큰이 만료되었습니다. 다시 로그인해주세요."),
    TOKEN_IS_INVALID("TOKEN-IS-INVALID", "토큰이 유효하지 않습니다."),
    PERMISSION_DENY("PERMISSION-DENY", "권한이 없습니다."),
    LOGIN_SUCCESS("LOGIN-SUCCESS", "로그인을 성공했습니다."),
    SIGNUP_SUCCESS("SIGNUP-SUCCESS", "회원가입을 성공했습니다"),
    LOGIN_FAIL("LOGIN-FAIL", "로그인을 실패했습니다."),

    /**
     * strategy
     */
    GET_STRATEGIES_SUCCESS("GET_STRATEGIES_SUCCESS", "전략 전체 조회를 성공했습니다."),
    GET_MONO_STRATEGY_SUCCESS("GET_MONO_STRATEGY_SUCCESS", "단일 조회 전략 조회를 성공했습니다.");

    private final String code;
    private final String message;
}
