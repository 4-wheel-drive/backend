package com.pda.trading_service.service.kis;

import com.pda.common_service.repository.KisTokenReader;
import com.pda.common_service.user.domain.Member;
import com.pda.trading_service.service.dto.KisDailyCcldResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisTradeExecutionService {
    private final KisTokenReader kisTokenReader;
    private final WebClient webClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * KIS 체결내역 조회 (회원별)
     */
    public KisDailyCcldResponse getDailyCcld(LocalDate date, Member member) {
        String formattedDate = date.format(DATE_FMT);

        String appKey = member.getMemberAppKey();
        String appSecret = member.getMemberAppSecret();
        String accessToken = kisTokenReader.getMemberAccessToken(member.getId());
        String accountNo = member.getMemberAccountNumber();

        log.info("[KIS] 체결내역 조회 요청 - 계좌: {}, 날짜: {}", accountNo, formattedDate);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/trading/inquire-daily-ccld")
                        .queryParam("CANO", accountNo)
                        .queryParam("ACNT_PRDT_CD", "01") // 뒤 2자리 상품코드
                        .queryParam("INQR_DVSN", "00")
                        .queryParam("ORD_DT", formattedDate)
                        .queryParam("SLL_BUY_DVSN_CD", "00")
                        .queryParam("INQR_DVSN_3", "00")
                        .queryParam("INQR_DVSN_1", "0")
                        .queryParam("CTX_AREA_FK100", "")
                        .queryParam("CTX_AREA_NK100", "")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "VTTCCTTTT") // 모의투자 체결조회 TR
                .retrieve()
                .bodyToMono(KisDailyCcldResponse.class)
                .doOnNext(res -> log.info("[KIS] 체결내역 응답 수신 - {}건",
                        res != null && res.output1() != null ? res.output1().size() : 0))
                .onErrorResume(e -> {
                    log.error("[KIS] 체결조회 API 호출 오류: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
    }
}