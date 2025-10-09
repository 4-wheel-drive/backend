package com.pda.auth_service.repository;

public interface KisTokenWriter {
    void saveAccessToken(Long memberId, String token, long ttlSeconds);
    void saveAdminApprovalKey(String approvalKey, long ttlSeconds);
}
