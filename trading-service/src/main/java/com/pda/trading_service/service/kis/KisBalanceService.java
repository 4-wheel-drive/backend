package com.pda.trading_service.service.kis;

import com.pda.common_service.exception.KisException;
import com.pda.common_service.repository.KisTokenReader;
import com.pda.common_service.response.ResponseMessage;
import com.pda.trading_service.service.kis.dto.KisBalanceResponse;
import com.pda.common_service.user.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@Service
public class KisBalanceService {

    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";
    private static final String BALANCE_PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
    private static final String TR_ID_VIRTUAL = "VTTC8434R";

    private final KisTokenReader kisTokenReader;
    private final RestTemplate restTemplate;

    /**
     * 🇰🇷 한국투자증권 모의투자 잔고 및 예수금 조회
     */
    public KisBalanceResponse getBalance(Member member) {
        String accessToken = kisTokenReader.getMemberAccessToken(member.getId());
        String appKey = member.getMemberAppKey();
        String appSecret = member.getMemberAppSecret();
        String accountNumber = member.getMemberAccountNumber();

        try {
            String cano = accountNumber.substring(0, 8);
            String prdtCd = accountNumber.substring(8);

            String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + BALANCE_PATH)
                    .queryParam("CANO", cano)
                    .queryParam("ACNT_PRDT_CD", prdtCd)
                    .queryParam("AFHR_FLPR_YN", "N")
                    .queryParam("OFL_YN", "N")
                    .queryParam("INQR_DVSN", "01")
                    .queryParam("UNPR_DVSN", "01")
                    .queryParam("FUND_STTL_ICLD_YN", "N")
                    .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                    .queryParam("PRCS_DVSN", "01")
                    .queryParam("CTX_AREA_FK100", "")
                    .queryParam("CTX_AREA_NK100", "")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + accessToken);
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", TR_ID_VIRTUAL);
            headers.set("custtype", "P");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KisBalanceResponse> responseEntity =
                    restTemplate.exchange(uri, HttpMethod.GET, entity, KisBalanceResponse.class);

            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("모의투자 잔고조회 실패 | 계좌: {}, 이유: {}", accountNumber, e.getMessage());
            throw new KisException(ResponseMessage.ORDER_CREATE_FAIL);
        }
    }
}
