package com.pda.common_service.response;

import com.pda.common_service.exception.StrategyException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ResponseMessage {

    /*
     * member
     * */
    MEMBER_NOT_FOUND("MEMBER-NOT-FOUND", "존재하지 않는 유저입니다"),
    MEMBER_ALREADY_EXISTED("MEMBER_ALREADY_EXISTED", "이미 존재하는 유저입니다"),

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
    ISSUE_KIS_APPROVAL_KEY_FAIL("ISSUE_APPROVAL_KEY_FAIL", "KIS 웹소켓 키 발급을 실패했습니다"),
    ISSUE_KIS_ACCESS_TOKEN_FAIL("ISSUE_KIS_ACCESS_TOKEN_FAIL", "KIS 접근 토큰 발급을 실패했습니다."),

    /**
     * strategy
     */
    GET_STRATEGIES_SUCCESS("GET_STRATEGIES_SUCCESS", "전략 전체 조회를 성공했습니다."),
    GET_MONO_STRATEGY_SUCCESS("GET_MONO_STRATEGY_SUCCESS", "단일 조회 전략 조회를 성공했습니다."),
    STRATEGY_NOT_FOUND("STRATEGY_NOT_FOUND", "존재하지 않는 전략입니다."),
    STRATEGY_SAVE_FAILED("STRATEGY-SAVE-FAILED", "전략 저장에 실패했습니다."),
    STRATEGY_SAVE_SUCCESS("STRATEGY-SAVE-SUCCESS", "전략 저장에 성공했습니다."),
    STRATEGY_DELETE_SUCCESS("STRATEGY_DELETE_SUCCESS", "전략 삭제를 성공했습니다."),
    GET_STRATEGY_MEMBER_SUCCESS("GET_STRATEGY_MEMBER_SUCCESS", "전략의 유저 조회를 성공했습니다."),



    /**
     * strategy templates
     */
    STRATEGY_TEMPLATE_SAVE_FAILED("STRATEGY-TEMPLATE-SAVE-FAILED", "전략 템플릿 저장에 실패했습니다."),
    STRATEGY_TEMPLATE_LOAD_FAILED("STRATEGY-TEMPLATE-LOAD-FAILED", "전략 템플릿 로드에 실패했습니다."),
    STRATEGY_TEMPLATE_INITIALIZE_FAILED("STRATEGY-TEMPLATE-INITIALIZE-FAILED", "전략 템플릿 초기화에 실패했습니다."),
    STRATEGY_TEMPLATE_UPDATE_FAILED("STRATEGY-TEMPLATE-UPDATE-FAILED", "전략 템플릿 업데이트에 실패했습니다."),
    STRATEGY_TEMPLATE_FILE_NOT_FOUND("STRATEGY-TEMPLATE-FILE-NOT-FOUND", "전략 템플릿 파일을 찾을 수 없습니다."),
    STRATEGY_TEMPLATE_FILE_READ_FAILED("STRATEGY-TEMPLATE-FILE-READ-FAILED", "전략 템플릿 파일 읽기에 실패했습니다."),
    STRATEGY_TEMPLATE_FORCE_REINITIALIZE_FAILED("STRATEGY-TEMPLATE-FORCE-REINITIALIZE-FAILED",
            "전략 템플릿 강제 재초기화에 실패했습니다."),
    GET_STRATEGY_TEMPLATES("GET_STRATEGY_TEMPLATES", "원클릭 템플릿 조회를 성공했습니다."),


    /**
     * trade
     */
    GET_EXECUTION_SUCCESS("GET_EXECUTION_SUCCESS", "체결 내역 가져오기를 성공했습니다."),

    /**
     * STOCK
     */
    STOCK_NOT_FOUND("STOCK_NOT_FOUND", "존재하지 않는 주식입니다"),
    GET_ALL_STOCKS_SUCCESS("GET_ALL_STOCKS_SUCCESS", "종목 전체 조회에 성공했습니다."),
    ALL_STOCK_FETCH_FAILED("ALL_STOCKS_FETCH_FAILED", "종목 전체 조회에 실패했습니다."),
    GET_MY_STOCKS_SUCCESS("GET_MY_STOCKS_SUCCESS", "내 종목 조회에 성공했습니다."),
    MY_STOCKS_FETCH_FAILED("MY_STOCKS_FETCH_FAILED", "내 종목 조회에 실패했습니다."),

    /**
     * Dashboard
     */
    GET_RETURNS_SUCCESS("GET_RETURNS_SUCCESS", "계좌 수익률 조회에 성공했습니다."),
    GET_PROFIT_RATE_RANKING_SUCCESS("GET_PROFIT_RATE_RANKING_SUCCESS", "수익률 랭킹 조회에 성공했습니다."),
    GET_STOCKS_SUCCESS("GET_STOCKS_SUCCESS", "보유 종목 조회에 성공했습니다."),
    GET_STOCKS_PROFIT_SUCCESS("GET_STOCKS_PROFIT_SUCCESS", "종목별 수익률 조회에 성공했습니다."),
    GET_TRANSACTIONS_SUCCESS("GET_TRANSACTIONS_SUCCESS", "전체 거래 체결 내역 조회에 성공했습니다."),
    GET_TRANSACTIONS_BY_STOCK_SUCCESS("GET_TRANSACTIONS_BY_STOCK_SUCCESS", "종목별 거래 체결 내역 조회에 성공했습니다."),

    /**
     * Order
     */
    ORDER_CREATE_SUCCESS("ORDER_CREATE_SUCCESS", "주문 생성 완료"),
    ORDER_CREATE_FAIL("ORDER_CREATE_FAIL", "주문 생성 실패"),
    DEPOSIT_DEFICIENT("DEPOSIT_DEFICIENT", "예수금이 부족합니다"),
    STOCK_QUANTITY_DEFICIENT("STOCK_QUANTITY_DEFICIENT", "보유 주식이 부족합니다"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다");

    private final String code;
    private final String message;
}
