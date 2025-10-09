package com.pda.auth_service.service.kis;

import com.pda.auth_service.repository.KisTokenRedisRepository;
import com.pda.auth_service.service.kis.dto.KisApprovalResponse;
import com.pda.auth_service.service.kis.dto.KisTokenResponse;
import com.pda.common_service.exception.KisException;
import com.pda.common_service.response.ResponseMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class KisAuthService {
    private static final String TOKEN_URL = "https://openapivts.koreainvestment.com:29443/oauth2/tokenP";
    private static final String APPROVAL_URL = "https://openapivts.koreainvestment.com:29443/oauth2/Approval";

    private static final long ACCESS_TOKEN_TTL = 24 * 60 * 60;
    private static final long APPROVAL_KEY_TTL = 6 * 60 * 60;

    private final RestTemplate restTemplate;
    private final KisTokenRedisRepository kisTokenRedisRepository;

    public KisTokenResponse saveMemberAccessToken(Long memberId, String appKey, String appSecret) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        KisTokenResponse response = restTemplate.postForObject(TOKEN_URL, request, KisTokenResponse.class);

        if (response == null || response.getAccessToken() == null) {
            throw new KisException(ResponseMessage.ISSUE_KIS_ACCESS_TOKEN_FAIL);
        }

        kisTokenRedisRepository.saveAccessToken(memberId, response.getAccessToken(), ACCESS_TOKEN_TTL);

        return response;
    }

    public KisApprovalResponse saveApprovalToken(String appKey, String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "secretkey", secretKey
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        KisApprovalResponse response = restTemplate.postForObject(APPROVAL_URL, request, KisApprovalResponse.class);

        if (response == null || response.getApprovalKey() == null) {
            throw new KisException(ResponseMessage.ISSUE_KIS_APPROVAL_KEY_FAIL);
        }

        kisTokenRedisRepository.saveAdminApprovalKey(response.getApprovalKey(), APPROVAL_KEY_TTL);

        return response;
    }

    public String getOrRefreshAccessToken(Long memberId, String appKey, String appSecret) {
        String cached = kisTokenRedisRepository.getMemberAccessToken(memberId);

        if (cached != null) {
            return cached;
        }

        return saveMemberAccessToken(memberId, appKey, appSecret).getAccessToken();
    }

    public String getOrRefreshApprovalKey(String appKey, String secretKey) {
        String cached = kisTokenRedisRepository.getAdminApprovalKey();

        if (cached != null) {
            return cached;
        }

        return saveApprovalToken(appKey, secretKey).getApprovalKey();
    }
}
