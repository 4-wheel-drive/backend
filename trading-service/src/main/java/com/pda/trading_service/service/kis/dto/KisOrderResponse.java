package com.pda.trading_service.service.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KisOrderResponse {

    @JsonProperty("rt_cd")
    private String resultCode;  // "0" = 정상

    @JsonProperty("msg_cd")
    private String messageCode;

    @JsonProperty("msg1")
    private String message;

    @JsonProperty("output")
    private Output output;

    @Data
    public static class Output {
        @JsonProperty("KRX_FWDG_ORD_ORGNO")
        private String exchangeCode;

        @JsonProperty("ODNO")
        private String orderNumber;

        @JsonProperty("ORD_TMD")
        private String orderTime;
    }

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
