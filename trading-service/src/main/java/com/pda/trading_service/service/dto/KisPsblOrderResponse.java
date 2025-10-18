package com.pda.trading_service.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisPsblOrderResponse(
        @JsonProperty("output") Output output
) {
    public record Output(
            @JsonProperty("ord_psbl_cash") String ord_psbl_cash // 매수가능금액
    ) {}
}

