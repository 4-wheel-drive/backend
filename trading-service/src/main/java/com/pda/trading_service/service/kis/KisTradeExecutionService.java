package com.pda.trading_service.service.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pda.common_service.repository.KisTokenReader;
import com.pda.common_service.user.domain.dto.MemberDto;
import com.pda.trading_service.controller.dto.StrategyMetaDto;
import com.pda.trading_service.controller.dto.StrategyWithMemberDto;
import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.execution.TradeExecutionStatus;
import com.pda.trading_service.domain.order.OrderStatus;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.service.dto.KisDailyCcldResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisTradeExecutionService {

    private final KisTokenReader kisTokenReader;
    private final WebClient webClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 일자별 체결내역 조회 (모의투자용)
     */
    public KisDailyCcldResponse getDailyExecution(LocalDate date, StrategyMetaDto strategyMetaDto, MemberDto memberInfo,
                                                  StockOrder stockOrder) {
        String formattedDate = date.format(DATE_FMT);

        String appKey = memberInfo.appKey();
        String appSecret = memberInfo.appSecret();
        String accessToken = kisTokenReader.getMemberAccessToken(memberInfo.id());
        String accountNo = memberInfo.memberAccountNumber();
        String stockCode = strategyMetaDto.stockId();
        String orderNo = stockOrder.getTradeId();

        log.info("""
                [KIS 요청 파라미터]
                CANO={} 
                ACNT_PRDT_CD=01 
                INQR_STRT_DT={} 
                INQR_END_DT={} 
                SLL_BUY_DVSN_CD=00 
                PDNO={} 
                ODNO={} 
                ORD_GNO_BRNO=00000 
                INQR_DVSN_3=00 
                INQR_DVSN_1=0 
                CTX_AREA_FK100='' 
                CTX_AREA_NK100=''
                """, accountNo, formattedDate, formattedDate, stockCode, orderNo);

        log.info("[KIS 요청 헤더] appKey={}, appSecret(길이)={}, accessToken(앞6자리)={}",
                appKey, appSecret.length(), accessToken != null ? accessToken.substring(0, 6) : "null");

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/trading/inquire-daily-ccld")
                            .queryParam("CANO", accountNo)
                            .queryParam("ACNT_PRDT_CD", "01")
                            .queryParam("INQR_STRT_DT", formattedDate)
                            .queryParam("INQR_END_DT", formattedDate)
                            .queryParam("SLL_BUY_DVSN_CD", "00")
                            .queryParam("CCLD_DVSN", "00")
                            .queryParam("PDNO", stockCode)
                            .queryParam("ODNO", orderNo)
                            .queryParam("ORD_GNO_BRNO", "00000")
                            .queryParam("INQR_DVSN", "0")
                            .queryParam("INQR_DVSN_3", "00")
                            .queryParam("INQR_DVSN_1", "0")
                            .queryParam("CTX_AREA_FK100", "")
                            .queryParam("CTX_AREA_NK100", "")
                            .build())
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", "VTTC0081R") // 모의투자용 체결내역조회 TR
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(raw -> log.info("[KIS 응답 원문]: {}", raw))
                    .map(body -> {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode root = mapper.readTree(body);
                            return mapper.readValue(body, KisDailyCcldResponse.class);

                        } catch (Exception e) {
                            log.error("[KIS 파싱 실패]: {}", e.getMessage());
                            return null;
                        }
                    })
                    .delaySubscription(Duration.ofMillis(1000))
                    .retryWhen(
                            Retry.backoff(3, Duration.ofMillis(500))
                                    .filter(e -> {
                                        if (e instanceof WebClientResponseException we) {
                                            log.warn("[KIS] 응답 오류 코드: {}", we.getStatusCode());
                                            return true;
                                        }
                                        String msg = e.getMessage();
                                        return msg != null && (
                                                msg.contains("EGW00201") ||
                                                        msg.contains("EGW00123") ||
                                                        msg.contains("EGW00111")
                                        );
                                    })
                                    .onRetryExhaustedThrow((spec, signal) ->
                                            new RuntimeException("[KIS] 재시도 초과: " + signal.failure().getMessage()))
                    )
                    .doOnNext(res -> log.info("[KIS] 체결내역 응답 수신 - {}건",
                            res != null && res.output1() != null ? res.output1().size() : 0))
                    .doOnError(err -> log.error("[KIS] API 호출 실패: {}", err.getMessage()))
                    .block();

        } catch (Exception e) {
            log.error("[KIS] 예외 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 체결 상태 확인 및 TradeExecution 생성
     */
    public TradeExecution checkTradeExecution(StockOrder stockOrder, LocalDate date,
                                              StrategyWithMemberDto strategyWithMember) {
        StrategyMetaDto strategyMetaDto = strategyWithMember.strategyMetaDto();
        MemberDto memberDto = strategyWithMember.memberDto();

        log.info("[KIS] 체결내역 조회 일자(어제): {}", date);

        KisDailyCcldResponse response = getDailyExecution(date, strategyMetaDto, memberDto, stockOrder);

        if (response == null || response.output1() == null) {
            log.warn("[KIS] 체결내역 응답이 비어있음 → PENDING 처리");
            return TradeExecution.create(stockOrder, TradeExecutionStatus.PENDING, 0, 0.0, 0.0);
        }

        Optional<KisDailyCcldResponse.Output1> orderData = response.output1().stream()
                .filter(o -> o.orderNo().equals(stockOrder.getTradeId()))
                .findFirst();

        if (orderData.isEmpty()) {
            log.warn("[KIS] 해당 주문번호({}) 체결정보 없음 → PENDING", stockOrder.getTradeId());
            return TradeExecution.create(stockOrder, TradeExecutionStatus.PENDING, 0, 0.0, 0.0);
        }

        KisDailyCcldResponse.Output1 info = orderData.get();

        int orderedQty = Integer.parseInt(info.orderQuantity());
        int filledQty = Integer.parseInt(info.filledQuantity());
        double avgPrice = Double.parseDouble(info.avgPrice());
        double totalAmount = avgPrice * filledQty;

        TradeExecutionStatus status;
        OrderStatus orderStatus;

        if (filledQty == 0) {
            status = TradeExecutionStatus.PENDING;
            orderStatus = OrderStatus.PENDING;
        } else if (filledQty < orderedQty) {
            status = TradeExecutionStatus.PARTIALLY_FILLED;
            orderStatus = OrderStatus.PARTIALLY_FILLED;
        } else {
            status = TradeExecutionStatus.FILLED;
            orderStatus = OrderStatus.FILLED;
        }

        log.info("[KIS 체결결과] 상태={} / 체결수량={} / 평균단가={} / 총금액={}",
                status, filledQty, avgPrice, totalAmount);

        stockOrder.updateStatus(orderStatus);
        return TradeExecution.create(stockOrder, status, filledQty, avgPrice, totalAmount);
    }

}
