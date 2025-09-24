package com.pda.common_service.authentication;

import com.pda.common_service.exception.AuthException;
import com.pda.common_service.response.ResponseMessage;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MemberOnlyChecker {
    @Before("@annotation(com.pda.common-service.authentication.MemberOnly)")
    public void check(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Accessor accessor && !accessor.isMember()) {
                throw new AuthException(ResponseMessage.PERMISSION_DENY);
            }
        }
    }
}

