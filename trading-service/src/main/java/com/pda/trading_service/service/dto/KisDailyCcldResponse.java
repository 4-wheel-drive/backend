package com.pda.trading_service.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KisDailyCcldResponse(
        @JsonProperty("output1") List<Output1> output1
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output1(
            @JsonProperty("odno") String orderNo,                     // 주문번호
            @JsonProperty("sll_buy_dvsn_cd") String side,             // 매도/매수 구분코드
            @JsonProperty("pdno") String productCode,                 // 종목코드
            @JsonProperty("prdt_name") String productName,            // 종목명
            @JsonProperty("ord_qty") String orderQuantity,            // 주문수량
            @JsonProperty("tot_ccld_qty") String filledQuantity,      // 체결수량
            @JsonProperty("avg_prvs") String avgPrice,                // 평균체결가
            @JsonProperty("tot_ccld_amt") String totalAmount          // 총체결금액
    ) {}
}
