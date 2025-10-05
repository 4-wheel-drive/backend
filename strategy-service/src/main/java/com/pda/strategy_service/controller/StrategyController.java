package com.pda.strategy_service.controller;

import com.pda.common_service.authentication.Accessor;
import com.pda.common_service.authentication.Auth;
import com.pda.common_service.authentication.MemberOnly;
import com.pda.common_service.response.ApiResponse;
import com.pda.common_service.response.ResponseMessage;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategies;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategy;
import com.pda.strategy_service.service.StrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/strategies")
public class StrategyController {
    private final StrategyService strategyService;

//    @MemberOnly
    @GetMapping()
//    public ResponseEntity<ApiResponse<ReadStrategies>> getStrategies(@Auth Accessor accessor) {
    public ResponseEntity<ApiResponse<ReadStrategies>> getStrategies() {
        Long memberId = 1L;
        ReadStrategies readStrategies = strategyService.getStrategies(memberId);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_STRATEGIES_SUCCESS.getCode(),
                        ResponseMessage.GET_STRATEGIES_SUCCESS.getMessage(),
                        readStrategies));
    }

    @GetMapping("/{strategyId}")
    public ResponseEntity<ApiResponse<ReadStrategy>> getStrategy(@PathVariable Long strategyId) {
        System.out.println("들어옴");
        ReadStrategy readStrategy = strategyService.getMonoStrategy(strategyId);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_STRATEGIES_SUCCESS.getCode(),
                        ResponseMessage.GET_STRATEGIES_SUCCESS.getMessage(),
                        readStrategy));
    }
}
