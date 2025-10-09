package com.pda.trading_service.service.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisSellPossibleResponse(
        @JsonProperty("output") Output output
) {
    public record Output(
            @JsonProperty("psbl_qty") String possibleQuantity,
            @JsonProperty("ord_psbl_cash") String orderPossibleCash,
            @JsonProperty("ord_psbl_cash_wan") String orderPossibleCashWon,
            @JsonProperty("item_name") String itemName,
            @JsonProperty("ord_psbl_qty") String orderPossibleQuantity,
            @JsonProperty("pchs_avg_pric") String purchaseAveragePrice,
            @JsonProperty("evlu_pfls_rt") String evaluationProfitRate
    ) {}
}
