package com.pda.common_service.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class ReadKisTokenRepository implements KisTokenReader {
    private final StringRedisTemplate redisTemplate;

    private static final String ACCESS_TOKEN_KEY_FORMAT = "kis:user:%s:access-token";
    private static final String ADMIN_APPROVAL_KEY = "kis:admin:approval-key";
    private static final String USER_APPROVAL_KEY = "kis:user:%s:approval-key";

    @Override
    public String getMemberAccessToken(Long memberId) {
        return redisTemplate.opsForValue().get(String.format(ACCESS_TOKEN_KEY_FORMAT, memberId));
    }

    @Override
    public String getAdminApprovalKey() {
        return redisTemplate.opsForValue().get(ADMIN_APPROVAL_KEY);
    }

    @Override
    public String getUserApprovalKey(Long memberId) {
        return redisTemplate.opsForValue().get(String.format(USER_APPROVAL_KEY, memberId));
    }
}
