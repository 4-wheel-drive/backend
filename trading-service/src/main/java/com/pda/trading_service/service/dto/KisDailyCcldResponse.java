package com.pda.trading_service.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KisDailyCcldResponse(
        @JsonProperty("output1") List<Output1> output1
) {
    public record Output1(
            @JsonProperty("ord_no") String orderNo,
            @JsonProperty("sll_buy_dvsn_cd") String side,
            @JsonProperty("pdno") String productCode,
            @JsonProperty("prdt_name") String productName,
            @JsonProperty("ord_qty") String orderQty,
            @JsonProperty("tot_ccld_qty") String executedQty,
            @JsonProperty("avg_prvs") String avgPrice
    ) {}
}
