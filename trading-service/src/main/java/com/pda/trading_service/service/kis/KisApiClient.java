package com.pda.trading_service.service.kis;

import com.pda.trading_service.service.kis.dto.KisOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KisApiClient {

    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";
    private static final String PRODUCT_CODE = "01"; // 일반계좌

    public KisOrderResponse sendBuyOrder(
            String appSecret, String appKey,
            String accountNumber,
            String accessToken, String stockCode, int quantity, BigDecimal price) {

        String url = BASE_URL + "/uapi/domestic-stock/v1/trading/order-cash";

        Map<String, Object> body = new HashMap<>();
        body.put("CANO", accountNumber);
        body.put("ACNT_PRDT_CD", PRODUCT_CODE);
        body.put("PDNO", stockCode);
        body.put("ORD_DVSN", "00"); // 00: 시장가
        body.put("ORD_QTY", quantity);
        body.put("ORD_UNPR", price.toPlainString());
        body.put("KRX_FWDG_ORD_ORGNO", "");
        body.put("ALGO_NO", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "VTTC0802U"); // 모의 매수

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<KisOrderResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                KisOrderResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("모의투자 매수 주문 실패: " + response.getStatusCode());
        }
    }

    public KisOrderResponse sendSellOrder(
            String appSecret, String appKey,
            String accountNumber,
            String accessToken, String stockCode, int quantity, BigDecimal price) {

        String url = BASE_URL + "/uapi/domestic-stock/v1/trading/order-cash";

        Map<String, Object> body = new HashMap<>();
        body.put("CANO", accountNumber);
        body.put("ACNT_PRDT_CD", PRODUCT_CODE);
        body.put("PDNO", stockCode);
        body.put("ORD_DVSN", "00");
        body.put("ORD_QTY", quantity);
        body.put("ORD_UNPR", price.toPlainString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "VTTC0801U"); // 모의 매도

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<KisOrderResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                KisOrderResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("모의투자 매도 주문 실패: " + response.getStatusCode());
        }
    }
}
