package com.pda.trading_service.service.kis;

import com.pda.common_service.exception.KisException;
import com.pda.common_service.repository.KisTokenReader;
import com.pda.common_service.response.ResponseMessage;
import com.pda.common_service.user.domain.Member;
import com.pda.trading_service.service.kis.dto.KisBalanceResponse;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class KisBalanceService {
    private static final String BASE_URL = "https://openapivts.koreainvestment.com:29443";
    private static final String BALANCE_PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
    private static final String TR_ID_VIRTUAL = "VTTC8434R";

    private final KisTokenReader kisTokenReader;
    private final WebClient kisWebClient;

    public Mono<BigDecimal> getAvailableCash(Member member) {
        String accessToken = kisTokenReader.getMemberAccessToken(member.getId());
        String appKey = member.getMemberAppKey();
        String appSecret = member.getMemberAppSecret();
        String accountNumber = member.getMemberAccountNumber();

        try {
            String cano = accountNumber.substring(0, 8);
            String prdtCd = accountNumber.substring(8);

            log.info("[KIS 잔고조회 요청] 계좌: {}, CANO: {}, PRDT_CD: {}", accountNumber, cano, prdtCd);

            return kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(BALANCE_PATH)
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
                            .build())
                    .header("authorization", "Bearer " + accessToken)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", TR_ID_VIRTUAL)
                    .header("custtype", "P")
                    .retrieve()
                    .bodyToMono(KisBalanceResponse.class)
                    .map(res -> {
                        BigDecimal available = new BigDecimal(res.orderPossibleCash());
                        BigDecimal safeAvailable = available.multiply(BigDecimal.valueOf(0.97)); // 여유 3% 남김
                        log.info("[KIS 매수가능금액 조회] 계좌: {}, 매수가능금액: {}, 안전잔고(97%): {}",
                                accountNumber, available, safeAvailable);
                        return safeAvailable;
                    })
                    .doOnError(WebClientResponseException.class, e -> {
                        log.error("[모의투자 잔고조회 실패] HTTP 오류: {}", e.getResponseBodyAsString());
                    })
                    .doOnError(e -> log.error("[모의투자 잔고조회 예외 발생] {}", e.getMessage()))
                    .onErrorResume(e -> {
                        throw new KisException(ResponseMessage.ORDER_CREATE_FAIL);
                    });

        } catch (Exception e) {
            log.error("[모의투자 잔고조회 실패] 계좌: {}, 사유: {}", accountNumber, e.getMessage());
            throw new KisException(ResponseMessage.ORDER_CREATE_FAIL);
        }
    }

    public Mono<KisBalanceResponse> getBalance(Member member) {
        String accessToken = kisTokenReader.getMemberAccessToken(member.getId());
        String appKey = member.getMemberAppKey();
        String appSecret = member.getMemberAppSecret();
        String accountNumber = member.getMemberAccountNumber();

        try {
            String cano = accountNumber.substring(0, 8);
            String prdtCd = accountNumber.substring(8);

            log.info("[KIS 보유주식조회 요청] 계좌: {}, CANO: {}, PRDT_CD: {}", accountNumber, cano, prdtCd);

            return kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(BALANCE_PATH)
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
                            .build())
                    .header("authorization", "Bearer " + accessToken)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", TR_ID_VIRTUAL)
                    .header("custtype", "P")
                    .retrieve()
                    .bodyToMono(KisBalanceResponse.class)
                    .doOnSuccess(res -> log.info("[모의투자 보유주식조회 성공] 계좌: {}, 종목수: {}",
                            accountNumber, res.balances() == null ? 0 : res.balances().size()))
                    .doOnError(WebClientResponseException.class,
                            e -> log.error("[모의투자 보유주식조회 실패] HTTP 오류: {}", e.getResponseBodyAsString()))
                    .doOnError(e -> log.error("[모의투자 보유주식조회 예외 발생] {}", e.getMessage()))
                    .onErrorMap(e -> new KisException(ResponseMessage.ORDER_CREATE_FAIL));

        } catch (Exception e) {
            log.error("[모의투자 보유주식조회 실패] 계좌: {}, 사유: {}", accountNumber, e.getMessage());
            throw new KisException(ResponseMessage.ORDER_CREATE_FAIL);
        }
    }

    public KisBalanceResponse getBalanceSync(Member member) {
        return getBalance(member).block();
    }

    /**
     * 🇰🇷 동기 버전
     */
    public BigDecimal getAvailableCashSync(Member member) {
        return getAvailableCash(member).block();
    }
}
