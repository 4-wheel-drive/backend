package com.pda.user_service.controller;

import com.pda.common_service.response.ApiResponse;
import com.pda.common_service.response.ResponseMessage;
import com.pda.user_service.controller.dto.AuthRequest;
import com.pda.user_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(
            @Valid @RequestBody AuthRequest.Login loginRequest
    ) {
        String id = loginRequest.memberId();
        String password = loginRequest.memberPassword();
        ResponseCookie responseCookie = authService.login(id, password);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(ApiResponse.success(
                        ResponseMessage.LOGIN_SUCCESS.getCode(),
                        ResponseMessage.LOGIN_SUCCESS.getMessage()
                ));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Object>> signUp(
            @Valid @RequestBody AuthRequest.SignUp signUpRequest
    ) {
        ResponseCookie responseCookie = authService.signUp(signUpRequest);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(ApiResponse.success(
                        ResponseMessage.SIGNUP_SUCCESS.getCode(),
                        ResponseMessage.SIGNUP_SUCCESS.getMessage()
                ));
    }
}
