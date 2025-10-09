package com.pda.auth_service.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class KisTokenRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String ACCESS_TOKEN_KEY_FORMAT = "kis:user:%s:access-token";
    private static final String ADMIN_APPROVAL_KEY = "kis:admin:approval-key";

    public void saveAccessToken(Long memberId, String token, long ttlSeconds) {
        String key = String.format(ACCESS_TOKEN_KEY_FORMAT, memberId);
        redisTemplate.opsForValue().set(key, token, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getMemberAccessToken(Long memberId) {
        String key = String.format(ACCESS_TOKEN_KEY_FORMAT, memberId);
        return redisTemplate.opsForValue().get(key);
    }

    public void saveAdminApprovalKey(String approvalKey, long ttlSeconds) {
        redisTemplate.opsForValue().set(ADMIN_APPROVAL_KEY, approvalKey, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getAdminApprovalKey() {
        return redisTemplate.opsForValue().get(ADMIN_APPROVAL_KEY);
    }
}
