package com.pda.trading_service.service.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KisBalanceResponse(
        @JsonProperty("output1") List<BalanceItem> balances,
        @JsonProperty("output2") List<Summary> summaries,

        @JsonProperty("ord_psbl_cash") String orderPossibleCash,
        @JsonProperty("dnca_tot_amt") String totalDepositAmount
) {
    public record BalanceItem(
            @JsonProperty("pdno") String productCode,
            @JsonProperty("prdt_name") String productName,
            @JsonProperty("trad_dvsn_name") String tradeTypeName,
            @JsonProperty("hldg_qty") String holdingQuantity,
            @JsonProperty("ord_psbl_qty") String orderableQuantity,
            @JsonProperty("pchs_avg_pric") String purchaseAvgPrice,
            @JsonProperty("pchs_amt") String purchaseAmount,
            @JsonProperty("prpr") String currentPrice,
            @JsonProperty("evlu_amt") String evaluationAmount,
            @JsonProperty("evlu_pfls_amt") String evaluationProfitAmount,
            @JsonProperty("evlu_pfls_rt") String evaluationProfitRate
    ) {}

    public record Summary(
            @JsonProperty("dnca_tot_amt") String totalDepositAmount,
            @JsonProperty("nxdy_excc_amt") String nextDaySettlementAmount,
            @JsonProperty("prvs_rcdl_excc_amt") String previousSettlementAmount
    ) {}
}
