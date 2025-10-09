package com.pda.auth_service.repository;

import com.pda.common_service.repository.KisTokenReader;
import com.pda.common_service.repository.KisTokenWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class KisTokenRedisRepository implements KisTokenReader, KisTokenWriter {
    private final StringRedisTemplate redisTemplate;

    private static final String ACCESS_TOKEN_KEY_FORMAT = "kis:user:%s:access-token";
    private static final String ADMIN_APPROVAL_KEY = "kis:admin:approval-key";

    @Override
    public void saveAccessToken(Long memberId, String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                String.format(ACCESS_TOKEN_KEY_FORMAT, memberId),
                token,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public String getMemberAccessToken(Long memberId) {
        return redisTemplate.opsForValue().get(String.format(ACCESS_TOKEN_KEY_FORMAT, memberId));
    }

    @Override
    public void saveAdminApprovalKey(String approvalKey, long ttlSeconds) {
        redisTemplate.opsForValue().set(ADMIN_APPROVAL_KEY, approvalKey, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String getAdminApprovalKey() {
        return redisTemplate.opsForValue().get(ADMIN_APPROVAL_KEY);
    }
}