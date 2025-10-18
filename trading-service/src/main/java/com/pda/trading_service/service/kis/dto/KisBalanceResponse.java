package com.pda.trading_service.service.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KisBalanceResponse(
        @JsonProperty("output1") List<BalanceItem> balances,
        @JsonProperty("output2") List<Summary> summaries
) {
    public record BalanceItem(
            @JsonProperty("pdno") String productCode,              // 종목코드
            @JsonProperty("prdt_name") String productName,         // 종목명
            @JsonProperty("trad_dvsn_name") String tradeTypeName,  // 매매구분명
            @JsonProperty("hldg_qty") String holdingQuantity,      // 보유수량
            @JsonProperty("ord_psbl_qty") String orderableQuantity,// 주문가능수량
            @JsonProperty("pchs_avg_pric") String purchaseAvgPrice,// 매입평균가
            @JsonProperty("pchs_amt") String purchaseAmount,       // 매입금액
            @JsonProperty("prpr") String currentPrice,             // 현재가
            @JsonProperty("evlu_amt") String evaluationAmount,     // 평가금액
            @JsonProperty("evlu_pfls_amt") String evaluationProfitAmount, // 평가손익금액
            @JsonProperty("evlu_pfls_rt") String evaluationProfitRate      // 평가손익율
    ) {}

    public record Summary(
            @JsonProperty("dnca_tot_amt") String totalDepositAmount,       // 예수금총금액
            @JsonProperty("nxdy_excc_amt") String nextDaySettlementAmount, // 익일정산금액
            @JsonProperty("prvs_rcdl_excc_amt") String previousSettlementAmount // 전일제비용정산금
    ) {}
}
