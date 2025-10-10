package com.pda.trading_service.service.kis;

import com.pda.trading_service.service.kis.dto.KisSellPossibleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisSellPossibleService {
    private static final String PATH = "/uapi/domestic-stock/v1/trading/inquire-psbl-sell";
    private static final String TR_ID_VIRTUAL = "VTTC8908R";

    private final WebClient kisWebClient;

    public KisSellPossibleResponse inquirePossibleSell(
            String accessToken,
            String appKey,
            String appSecret,
            String cano,
            String prdtCd,
            String pdno,
            String orderPrice
    ) {
        log.info("[KIS 모의투자 매도가능조회 요청] CANO={}, PRDT_CD={}, PDNO={}, PRICE={}", cano, prdtCd, pdno, orderPrice);

        try {
            KisSellPossibleResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PATH)
                            .queryParam("CANO", cano)
                            .queryParam("ACNT_PRDT_CD", prdtCd)
                            .queryParam("PDNO", pdno)
                            .queryParam("ORD_UNPR", orderPrice)
                            .queryParam("ORD_DVSN", "00")   // 00: 지정가
                            .queryParam("CCLD_DVSN", "00")  // 00: 일반체결
                            .build())
                    .header("authorization", "Bearer " + accessToken)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", TR_ID_VIRTUAL)
                    .header("custtype", "P")
                    .retrieve()
                    .bodyToMono(KisSellPossibleResponse.class)
                    .doOnSuccess(res -> log.info("✅ [모의투자 매도가능조회 성공] 종목: {}, 수량: {}",
                            pdno,
                            res.output() != null ? res.output().orderPossibleQuantity() : "0"))
                    .doOnError(WebClientResponseException.class, e ->
                            log.error("[HTTP 오류] {}", e.getResponseBodyAsString()))
                    .doOnError(e ->
                            log.error("[예외 발생] {}", e.getMessage()))
                    .block();

            return response;

        } catch (WebClientResponseException e) {
            log.error("🚨 [HTTP 예외 발생] 상태코드: {}, 메시지: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("💥 [매도가능조회 예외 발생] {}", e.getMessage(), e);
            throw e;
        }
    }
}
