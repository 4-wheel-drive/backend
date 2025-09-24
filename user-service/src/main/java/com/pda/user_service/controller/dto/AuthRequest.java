package com.pda.user_service.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthRequest {
    public record Login(
            @NotBlank(message = "ID는 공백이 될 수 없습니다")
            String memberId,

            @NotBlank(message = "PW는 공백이 될 수 없습니다")
            String password
    ) {
    }

    public record SignUp(
            @NotBlank(message = "ID는 공백이 될 수 없습니다")
            String memberId,

            @NotBlank(message = "PW는 공백이 될 수 없습니다")
            String memberPassword,

            @NotBlank(message = "이름은 공백이 될 수 없습니다")
            String memberName,

            @NotBlank(message = "계좌번호는 공백이 될 수 없습니다")
            String memberAccountNumber,

            @NotBlank(message = "app-key는 공백이 될 수 없습니다")
            String memberAppKey,

            @NotBlank(message = "app-secret은 공백이 될 수 없습니다")
            String memberAppSecret
    ) {
    }
}
