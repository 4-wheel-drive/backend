package com.pda.strategy_service.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisPsblOrderResponse(
        @JsonProperty("output") Output output
) {
    public record Output(
            @JsonProperty("ord_psbl_cash") String orderPossibleCash
    ) {}
}
