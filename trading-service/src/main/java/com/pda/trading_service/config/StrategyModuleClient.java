package com.pda.trading_service.config;

import com.pda.common_service.response.ApiResponse;
import com.pda.trading_service.controller.dto.StrategyWithMemberDto;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyModuleClient {
    private final WebClient webClient;

    @Value("${module.strategy-service.url}")
    private String strategyServiceUrl;

    public StrategyWithMemberDto getStrategyInfo(Long strategyId) {
        log.info("[HTTP] strategy-module 호출 → strategyId={}", strategyId);

        return Objects.requireNonNull(webClient.get()
                        .uri(strategyServiceUrl + "/api/v1/strategies/{strategyId}/info", strategyId)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponse<StrategyWithMemberDto>>() {
                        })
                        .block())
                .data();

    }
}
