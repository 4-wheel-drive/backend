package com.pda.trading_service.service.kis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.pda.trading_service.service.kis.dto.KisSellPossibleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisSellPossibleService {

    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";

    private final RestTemplate restTemplate;

    public KisSellPossibleResponse inquirePossibleSell(
            String accessToken,
            String appKey,
            String appSecret,
            String cano,
            String prdtCd,
            String pdno,
            String orderPrice
    ) {
        try {
            String url = BASE_URL + "/uapi/domestic-stock/v1/trading/inquire-psbl-sell";

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + accessToken);
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "VTTC8908R"); // 모의투자 TR ID
            headers.set("custtype", "P");

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("CANO", cano)
                    .queryParam("ACNT_PRDT_CD", prdtCd)
                    .queryParam("PDNO", pdno)
                    .queryParam("ORD_UNPR", orderPrice)
                    .queryParam("ORD_DVSN", "00")
                    .queryParam("CCLD_DVSN", "00");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KisSellPossibleResponse> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    KisSellPossibleResponse.class
            );

            KisSellPossibleResponse result = response.getBody();
            return result;

        } catch (Exception e) {
            throw new RuntimeException("KIS 매도가능조회 API 호출 실패", e);
        }
    }
}
