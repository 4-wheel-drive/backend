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

    /**
     * strategy templates
     */
    STRATEGY_TEMPLATE_SAVE_FAILED("STRATEGY-TEMPLATE-SAVE-FAILED", "전략 템플릿 저장에 실패했습니다."),
    STRATEGY_TEMPLATE_LOAD_FAILED("STRATEGY-TEMPLATE-LOAD-FAILED", "전략 템플릿 로드에 실패했습니다."),
    STRATEGY_TEMPLATE_INITIALIZE_FAILED("STRATEGY-TEMPLATE-INITIALIZE-FAILED", "전략 템플릿 초기화에 실패했습니다."),
    STRATEGY_TEMPLATE_UPDATE_FAILED("STRATEGY-TEMPLATE-UPDATE-FAILED", "전략 템플릿 업데이트에 실패했습니다."),
    STRATEGY_TEMPLATE_FILE_NOT_FOUND("STRATEGY-TEMPLATE-FILE-NOT-FOUND", "전략 템플릿 파일을 찾을 수 없습니다."),
    STRATEGY_TEMPLATE_FILE_READ_FAILED("STRATEGY-TEMPLATE-FILE-READ-FAILED", "전략 템플릿 파일 읽기에 실패했습니다."),
    STRATEGY_TEMPLATE_FORCE_REINITIALIZE_FAILED("STRATEGY-TEMPLATE-FORCE-REINITIALIZE-FAILED", "전략 템플릿 강제 재초기화에 실패했습니다.");

    private final String code;
    private final String message;
}
