package com.pda.user_service.service;

import com.pda.common_service.exception.AuthException;
import com.pda.common_service.response.ResponseMessage;
import com.pda.user_service.controller.dto.AuthRequest;
import com.pda.user_service.domain.Member;
import com.pda.user_service.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;

    @Override
    public ResponseCookie login(String memberId, String password) {
        Member member = memberRepository.findByMemberId(memberId).orElseThrow();
        String findMemberId = member.getMemberId();
        String findPassword = member.getMemberPassword();

        if (!memberId.equals(findMemberId)) {
            throw new AuthException(ResponseMessage.LOGIN_FAIL);
        }
        if (!BCrypt.checkpw(password, findPassword)) {
            throw new AuthException(ResponseMessage.LOGIN_FAIL);
        }
        String accessToken = tokenProvider.generateTokens(member.getId().toString());

        return TokenCookieManager.createCookie(accessToken);
    }

    @Override
    public ResponseCookie signUp(AuthRequest.SignUp signUpRequest) {
        String hashedPw = BCrypt.hashpw(signUpRequest.memberPassword(), BCrypt.gensalt());
        Member member = Member.create(signUpRequest.memberId(), hashedPw, signUpRequest.memberName(),
                signUpRequest.memberAccountNumber(), signUpRequest.memberAppKey(), signUpRequest.memberAppSecret());

        Member savedMember = memberRepository.save(member);
        String accessToken = tokenProvider.generateTokens(savedMember.getId().toString());

        return TokenCookieManager.createCookie(accessToken);
    }
}
