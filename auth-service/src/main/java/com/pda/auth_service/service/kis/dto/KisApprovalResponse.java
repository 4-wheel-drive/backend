package com.pda.auth_service.service.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KisApprovalResponse {
    @JsonProperty("approval_key")
    private String approvalKey;
}
