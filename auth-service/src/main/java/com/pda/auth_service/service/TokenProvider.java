package com.pda.auth_service.service;

import com.pda.common_service.util.TimeUtil;
import com.pda.auth_service.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenProvider {
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey secretKey;

    public String generateTokens(String subject) {
        return createToken(subject, jwtProperties.getAccessExpireMs());
    }

    public String createToken(String subject, Long expireMs) {
        Date now = TimeUtil.localDateTimeToDate(LocalDateTime.now(clock), clock);
        Date expiredTime = new Date(now.getTime() + expireMs);

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(subject)
                .issuedAt(now)
                .signWith(secretKey)
                .expiration(expiredTime)
                .compact();
    }

    public Jws<Claims> validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    public String getSubject(String token) {
        return validateToken(token)
                .getPayload()
                .getSubject();
    }
}

