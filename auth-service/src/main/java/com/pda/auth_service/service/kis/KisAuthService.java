package com.pda.auth_service.service.kis;

import com.pda.auth_service.repository.KisTokenRedisRepository;
import com.pda.auth_service.service.kis.dto.KisApprovalResponse;
import com.pda.auth_service.service.kis.dto.KisTokenResponse;
import com.pda.common_service.exception.KisException;
import com.pda.common_service.response.ResponseMessage;
import java.nio.channels.MembershipKey;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class KisAuthService {

    private static final String IMITATION_TOKEN_URL = "https://openapivts.koreainvestment.com:29443/oauth2/tokenP";
    private static final String IMITATION_APPROVAL_URL = "https://openapivts.koreainvestment.com:29443/oauth2/Approval";

    private static final String REAL_TOKEN_URL = "https://openapi.koreainvestment.com:9443/oauth2/tokenP";
    private static final String REAL_APPROVAL_URL = "https://openapi.koreainvestment.com:9443/oauth2/Approval";

    private static final long ACCESS_TOKEN_TTL = 24 * 60 * 60;
    private static final long APPROVAL_KEY_TTL = 6 * 60 * 60;

    private final WebClient webClient;
    private final KisTokenRedisRepository kisTokenRedisRepository;

    public KisTokenResponse requestAccessToken(Long memberId, String appKey, String appSecret) {
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        KisTokenResponse response = webClient.post()
                .uri(IMITATION_TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(KisTokenResponse.class)
                .onErrorMap(e -> new KisException(ResponseMessage.ISSUE_KIS_ACCESS_TOKEN_FAIL))
                .block();

        if (response == null || response.getAccessToken() == null) {
            throw new KisException(ResponseMessage.ISSUE_KIS_ACCESS_TOKEN_FAIL);
        }
        return response;
    }

    public KisTokenResponse saveMemberAccessToken(Long memberId, String appKey, String appSecret) {
        KisTokenResponse kisTokenResponse = requestAccessToken(memberId, appKey, appSecret);
        kisTokenRedisRepository.saveAccessToken(memberId, kisTokenResponse.getAccessToken(), ACCESS_TOKEN_TTL);
        return kisTokenResponse;
    }

    public KisApprovalResponse saveAdminApprovalToken(String appKey, String secretKey) {
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "secretkey", secretKey
        );

        KisApprovalResponse response = webClient.post()
                .uri(REAL_APPROVAL_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(KisApprovalResponse.class)
                .onErrorMap(e -> new KisException(ResponseMessage.ISSUE_KIS_APPROVAL_KEY_FAIL))
                .block();

        if (response == null || response.getApprovalKey() == null) {
            throw new KisException(ResponseMessage.ISSUE_KIS_APPROVAL_KEY_FAIL);
        }

        kisTokenRedisRepository.saveAdminApprovalKey(response.getApprovalKey(), APPROVAL_KEY_TTL);
        return response;
    }

    public KisTokenResponse saveAdminAccessToken(String appKey, String secretKey) {
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "secretkey", secretKey
        );

        KisTokenResponse response = webClient.post()
                .uri(REAL_APPROVAL_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(KisTokenResponse.class)
                .onErrorMap(e -> new KisException(ResponseMessage.ISSUE_KIS_ACCESS_TOKEN_FAIL))
                .block();

        if (response == null || response.getAccessToken() == null) {
            throw new KisException(ResponseMessage.ISSUE_KIS_ACCESS_TOKEN_FAIL);
        }

        kisTokenRedisRepository.saveAdminAccessToken(response.getAccessToken(), ACCESS_TOKEN_TTL);
        return response;
    }

    public String getOrRefreshMemberAccessToken(Long memberId, String appKey, String appSecret) {
        String cached = kisTokenRedisRepository.getMemberAccessToken(memberId);
        if (cached != null) {
            return cached;
        }
        return saveMemberAccessToken(memberId, appKey, appSecret).getAccessToken();
    }

    public String getOrRefreshAdminApprovalKey(String appKey, String secretKey) {
        String cached = kisTokenRedisRepository.getAdminApprovalKey();
        if (cached != null) {
            return cached;
        }
        return saveAdminApprovalToken(appKey, secretKey).getApprovalKey();
    }

//
//    public KisApprovalResponse saveUserApprovalToken(Long memberId, String appKey, String secretKey) {
//        Map<String, String> body = Map.of(
//                "grant_type", "client_credentials",
//                "appkey", appKey,
//                "secretkey", secretKey
//        );
//
//        KisApprovalResponse response = webClient.post()
//                .uri(APPROVAL_URL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(body)
//                .retrieve()
//                .bodyToMono(KisApprovalResponse.class)
//                .onErrorMap(e -> new KisException(ResponseMessage.ISSUE_KIS_APPROVAL_KEY_FAIL))
//                .block();
//
//        if (response == null || response.getApprovalKey() == null) {
//            throw new KisException(ResponseMessage.ISSUE_KIS_APPROVAL_KEY_FAIL);
//        }
//
//        kisTokenRedisRepository.saveUserApprovalKey(memberId, response.getApprovalKey(), APPROVAL_KEY_TTL);
//        return response;
//    }
}
