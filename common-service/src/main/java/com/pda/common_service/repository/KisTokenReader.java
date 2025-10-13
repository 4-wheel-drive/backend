package com.pda.common_service.repository;

public interface KisTokenReader {
    String getMemberAccessToken(Long memberId);
    String getAdminApprovalKey();
    String getUserApprovalKey(Long memberId);
}
