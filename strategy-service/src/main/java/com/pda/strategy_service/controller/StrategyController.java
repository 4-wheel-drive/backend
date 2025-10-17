package com.pda.strategy_service.controller;

import com.pda.common_service.authentication.Accessor;
import com.pda.common_service.authentication.Auth;
import com.pda.common_service.authentication.MemberOnly;
import com.pda.common_service.response.ApiResponse;
import com.pda.common_service.response.ResponseMessage;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.domain.dto.MemberDto;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategies;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategy;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.dto.StrategyMetaDto;
import com.pda.strategy_service.domain.dto.StrategyWithMemberDto;
import com.pda.strategy_service.domain.mongodb.CustomStrategy;
import com.pda.strategy_service.service.StrategyService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
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
        ReadStrategy readStrategy = strategyService.getMonoStrategy(1L, strategyId);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.GET_MONO_STRATEGY_SUCCESS.getCode(),
                        ResponseMessage.GET_MONO_STRATEGY_SUCCESS.getMessage(),
                        readStrategy));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomStrategy>> saveStrategy(@RequestBody Map<String, Object> strategyJson) {
        String strategyName = (String) strategyJson.get("strategy_name");
        Map<String, Object> meta = (Map<String, Object>) strategyJson.get("meta");
        List<String> universe = (List<String>) meta.get("universe");
        String stockId = (universe != null && !universe.isEmpty()) ? universe.get(0) : null;
        StrategyMetaDto strategyMeta = new StrategyMetaDto(stockId, strategyName);
        Strategy strategy = strategyService.saveStrategyMeta(1L, strategyMeta);
        CustomStrategy customStrategy = strategyService.saveStrategy(strategy.getId(), strategyJson);

        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.STRATEGY_SAVE_SUCCESS.getCode(),
                        ResponseMessage.STRATEGY_SAVE_SUCCESS.getMessage(),
                        customStrategy));
    }

    @DeleteMapping("/{strategyId}")
    public ResponseEntity<ApiResponse<String>> deleteStrategy(@PathVariable Long strategyId) {
        Long memberId = 1L;
        strategyService.deleteStrategyById(strategyId, memberId);
        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.STRATEGY_DELETE_SUCCESS.getCode(),
                        ResponseMessage.STRATEGY_DELETE_SUCCESS.getMessage()
                ));
    }

    @GetMapping("/{strategyId}/info")
    public ResponseEntity<ApiResponse<StrategyWithMemberDto>> getStrategyInfo(@PathVariable Long strategyId) {
        Long memberId = 1L;
        StrategyWithMemberDto foundMember = strategyService.getStrategyWithMember(strategyId, memberId);
        return ResponseEntity
                .ok()
                .body(ApiResponse.success(
                        ResponseMessage.STRATEGY_DELETE_SUCCESS.getCode(),
                        ResponseMessage.STRATEGY_DELETE_SUCCESS.getMessage(),
                        foundMember
                ));
    }
}
