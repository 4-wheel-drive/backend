package com.pda.auth_service.service;

import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.http.ResponseCookie;

public class TokenCookieManager {
    private static final String TOKEN_NAME = "access-token";
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(14L);

    public static String extractTokenOrNull(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(TOKEN_NAME))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    public static ResponseCookie createCookie(String tokenValue) {
        return ResponseCookie.from(TOKEN_NAME, tokenValue)
                .maxAge(COOKIE_MAX_AGE)
                .secure(true)
                .httpOnly(true)
                .sameSite("none")
                .path("/")
                .build();
    }

    public static ResponseCookie deleteCookie() {
        return ResponseCookie.from(TOKEN_NAME, "")
                .path("/")
                .maxAge(0)
                .secure(true)
                .httpOnly(true)
                .sameSite("none")
                .build();
    }
}
