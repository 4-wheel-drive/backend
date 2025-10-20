package com.pda.auth_service.service;

import com.pda.auth_service.controller.dto.AuthRequest;
import org.springframework.http.ResponseCookie;

public interface AuthService {
    ResponseCookie login(String id, String password);

    void signUp(AuthRequest.SignUp signUpRequest);
}
