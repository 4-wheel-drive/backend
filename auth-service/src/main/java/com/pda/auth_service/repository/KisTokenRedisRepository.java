package com.pda.auth_service.repository;

import com.pda.common_service.repository.KisTokenReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class KisTokenRedisRepository implements KisTokenReader, KisTokenWriter {
    private final StringRedisTemplate redisTemplate;

    private static final String USER_TOKEN_KEY = "kis:user:%s:access-token";
    private static final String USER_APPROVAL_KEY = "kis:user:%s:approval-key";

    private static final String ADMIN_APPROVAL_KEY = "kis:admin:approval-key";
    private static final String ADMIN_ACCESS_KEY = "kis:admin:access-token";

    @Override
    public void saveAccessToken(Long memberId, String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                String.format(USER_TOKEN_KEY, memberId),
                token,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public String getMemberAccessToken(Long memberId) {
        return redisTemplate.opsForValue().get(String.format(USER_TOKEN_KEY, memberId));
    }

    @Override
    public void saveAdminApprovalKey(String approvalKey, long ttlSeconds) {
        redisTemplate.opsForValue().set(ADMIN_APPROVAL_KEY, approvalKey, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void saveUserApprovalKey(Long memberId, String approvalKey, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                String.format(USER_APPROVAL_KEY, memberId),
                approvalKey,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public String getAdminApprovalKey() {
        return redisTemplate.opsForValue().get(ADMIN_APPROVAL_KEY);
    }

    @Override
    public String getUserApprovalKey(Long memberId) {
        return redisTemplate.opsForValue().get(String.format(USER_APPROVAL_KEY, memberId));
    }

    public void saveAdminAccessToken(String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                ADMIN_ACCESS_KEY,
                token,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }
}
