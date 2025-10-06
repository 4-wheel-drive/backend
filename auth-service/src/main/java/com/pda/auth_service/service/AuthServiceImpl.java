package com.pda.auth_service.service;

import com.pda.common_service.exception.AuthException;
import com.pda.common_service.exception.DuplicatedException;
import com.pda.common_service.response.ResponseMessage;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.repository.MemberRepository;
import com.pda.auth_service.controller.dto.AuthRequest;
import java.util.Optional;
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
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new AuthException(ResponseMessage.LOGIN_FAIL));

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

        Optional<Member> optionalMember = memberRepository.findByMemberId(signUpRequest.memberId());

        if (optionalMember.isPresent()) {
            throw new DuplicatedException(ResponseMessage.MEMBER_ALREADY_EXISTED);
        }

        Member member = Member.create(signUpRequest.memberId(), hashedPw, signUpRequest.memberName(),
                signUpRequest.memberAccountNumber(), signUpRequest.memberAppKey(), signUpRequest.memberAppSecret());

        Member savedMember = memberRepository.save(member);
        String accessToken = tokenProvider.generateTokens(savedMember.getId().toString());

        return TokenCookieManager.createCookie(accessToken);
    }
}
