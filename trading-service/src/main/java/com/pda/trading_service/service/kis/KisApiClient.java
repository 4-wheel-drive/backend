package com.pda.trading_service.service.kis;

import com.pda.trading_service.service.kis.dto.KisOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisApiClient {

    private final WebClient kisWebClient;

    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";
    private static final String ORDER_PATH = "/uapi/domestic-stock/v1/trading/order-cash";
    private static final String PRODUCT_CODE = "01"; // 일반계좌

    /**
     * 🇰🇷 모의투자 시장가 주문 공통 메서드
     */
    private Mono<KisOrderResponse> sendOrder(
            String appSecret,
            String appKey,
            String accountNumber,
            String accessToken,
            String stockCode,
            int quantity,
            BigDecimal price,
            String trId,
            String orderType // BUY or SELL
    ) {
        String cano = accountNumber.substring(0, 8);
        String prdtCd = accountNumber.substring(8);

        Map<String, String> body = new HashMap<>();
        body.put("CANO", cano);
        body.put("ACNT_PRDT_CD", prdtCd);
        body.put("PDNO", stockCode);
        body.put("ORD_DVSN", "01");     // ✅ 시장가
        body.put("ORD_QTY", String.valueOf(quantity));
        body.put("ORD_UNPR", "0");      // ✅ 시장가 주문은 0 고정
        body.put("KRX_FWDG_ORD_ORGNO", "");
        body.put("ALGO_NO", "");

        log.info("[KIS 모의투자 {} 요청]: {}", orderType, body);

        return kisWebClient.post()
                .uri(BASE_URL + ORDER_PATH)
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", trId)
                .header("custtype", "P")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("[KIS 서버오류 5xx] {}", errorBody);
                                    return Mono.error(new RuntimeException("KIS 서버 오류: " + errorBody));
                                })
                )
                .bodyToMono(KisOrderResponse.class)
                .doOnNext(res -> log.info("[KIS 응답]: {}", res))
                .doOnError(e -> log.error("[{}] 주문 실패: {}", orderType, e.getMessage()));
    }


    /**
     * ✅ 모의투자 매수 (시장가)
     */
    public Mono<KisOrderResponse> sendBuyOrder(
            String appSecret,
            String appKey,
            String accountNumber,
            String accessToken,
            String stockCode,
            int quantity,
            BigDecimal price
    ) {
        return sendOrder(appSecret, appKey, accountNumber, accessToken, stockCode, quantity, price,
                "VTTC0802U", "BUY");
    }

    /**
     * ✅ 모의투자 매도 (시장가)
     */
    public Mono<KisOrderResponse> sendSellOrder(
            String appSecret,
            String appKey,
            String accountNumber,
            String accessToken,
            String stockCode,
            int quantity,
            BigDecimal price
    ) {
        return sendOrder(appSecret, appKey, accountNumber, accessToken, stockCode, quantity, price,
                "VTTC0801U", "SELL");
    }
}
