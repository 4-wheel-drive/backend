package com.pda.trading_service.service.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisOrderResponse(
        @JsonProperty("responseHeader")
        ResponseHeader responseHeader,

        @JsonProperty("responseBody")
        ResponseBody responseBody
) {
    public record ResponseHeader(
            @JsonProperty("content-type")
            String contentType
    ) {}

    public record ResponseBody(
            @JsonProperty("rt_cd")
            String resultCode,       // 성공 실패 여부 ("0" = 성공)

            @JsonProperty("msg_cd")
            String messageCode,      // 응답 코드

            @JsonProperty("msg1")
            String message,          // 응답 메시지

            @JsonProperty("output")
            ResponseBodyOutput output // ✅ 단일 객체로 변경
    ) {}

    public record ResponseBodyOutput(
            @JsonProperty("KRX_FWDG_ORD_ORGNO")
            String exchangeCode,    // 거래소 코드

            @JsonProperty("ODNO")
            String orderNumber,     // 주문번호

            @JsonProperty("ORD_TMD")
            String orderTime        // 주문시간
    ) {}
}
