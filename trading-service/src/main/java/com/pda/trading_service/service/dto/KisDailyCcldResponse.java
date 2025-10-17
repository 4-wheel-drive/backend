package com.pda.trading_service.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KisDailyCcldResponse(
        String rt_cd,
        String msg_cd,
        String msg1,
        List<Output1> output1
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output1(
            String orderNo,
            String orderQuantity,
            String filledQuantity,
            String avgPrice
    ) {}
}
