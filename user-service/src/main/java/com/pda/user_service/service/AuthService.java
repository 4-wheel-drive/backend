package com.pda.user_service.service;

import com.pda.user_service.controller.dto.AuthRequest;
import org.springframework.http.ResponseCookie;

public interface AuthService {
    ResponseCookie login(String id, String password);

    ResponseCookie signUp(AuthRequest.SignUp signUpRequest);

}
