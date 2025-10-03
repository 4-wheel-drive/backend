package com.pda.auth_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pda.common_service.response.ApiResponse;
import com.pda.common_service.response.ResponseMessage;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        try {
            String accessTokenValue = TokenCookieManager.extractTokenOrNull(request.getCookies());

            if (accessTokenValue == null) {
                request.setAttribute("role", "GUEST");
                return true;
            }

            String userId = tokenProvider.getSubject(accessTokenValue);

            request.setAttribute("userId", userId);
            request.setAttribute("role", "USER");

            return true;
        } catch (ExpiredJwtException e) {
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ResponseMessage.TOKEN_IS_EXPIRED);
            return false;
        } catch (JwtException e) {
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ResponseMessage.TOKEN_IS_INVALID);
            return false;
        }
    }

    private void setErrorResponse(HttpServletResponse response,
                                  int httpStatus,
                                  ResponseMessage responseMessage) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> errorResponse = ApiResponse.failure(responseMessage.getCode(),
                responseMessage.getMessage());

        String json = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(json);
    }
}
