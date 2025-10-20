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
import com.pda.strategy_service.service.StrategyCodeGenerator;
import com.pda.strategy_service.service.StrategyCodeSummaryService;
import com.pda.strategy_service.service.StrategyService;
import com.pda.strategy_service.service.StrategyRunnerService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/strategies")
public class StrategyController {
  private final StrategyService strategyService;
  private final StrategyRunnerService strategyRunnerService;
  private final StrategyCodeGenerator strategyCodeGenerator;
  private final StrategyCodeSummaryService strategyCodeSummaryService;

  // @MemberOnly
  @GetMapping()
  // public ResponseEntity<ApiResponse<ReadStrategies>> getStrategies(@Auth
  // Accessor accessor) {
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

    // 코드 생성
    String summarizeCode = strategyCodeGenerator.generateCode(strategyJson, 1L, stockId);

    // 코드 summary and save
    strategyCodeSummaryService.generateSummaryAndSave(strategy.getId(), summarizeCode);

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
            ResponseMessage.STRATEGY_DELETE_SUCCESS.getMessage()));
  }

  /**
   * 전략 실행 시작
   */
  @PostMapping("/{strategyId}/start")
  public ResponseEntity<ApiResponse<Map<String, String>>> startStrategy(
      @PathVariable Long strategyId,
      @RequestParam(defaultValue = "1") Long memberId) {

    String podName = strategyRunnerService.strategyStart(strategyId, memberId);

    return ResponseEntity
        .ok()
        .body(ApiResponse.success(
            "STRATEGY_START_SUCCESS",
            "전략 실행이 시작되었습니다.",
            Map.of(
                "strategyId", strategyId.toString(),
                "podName", podName,
                "status", "ACTIVATED")));
  }

  /**
   * 전략 실행 중지
   */
  @PostMapping("/{strategyId}/stop")
  public ResponseEntity<ApiResponse<Map<String, String>>> stopStrategy(
      @PathVariable Long strategyId,
      @RequestParam(defaultValue = "1") Long memberId) {

    strategyRunnerService.strategyStop(strategyId, memberId);

    return ResponseEntity
        .ok()
        .body(ApiResponse.success(
            "STRATEGY_STOP_SUCCESS",
            "전략 실행이 중지되었습니다.",
            Map.of(
                "strategyId", strategyId.toString(),
                "status", "STOPPED")));
  }

  /**
   * 전략 실행 상태 확인
   */
  @GetMapping("/{strategyId}/status")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getStrategyStatus(
      @PathVariable Long strategyId,
      @RequestParam(defaultValue = "1") Long memberId) {

    boolean isRunning = strategyRunnerService.isStrategyRunning(strategyId, memberId);

    return ResponseEntity
        .ok()
        .body(ApiResponse.success(
            "STRATEGY_STATUS_SUCCESS",
            "전략 상태 조회 성공",
            Map.of(
                "strategyId", strategyId,
                "isRunning", isRunning,
                "status", isRunning ? "RUNNING" : "STOPPED")));
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
            foundMember));
  }

  /**
   * 전략 파이썬 코드 미리보기 (개발용)
   * 실제 Pod를 생성하지 않고 생성될 파이썬 코드만 확인
   */
  @GetMapping("/{strategyId}/preview-code")
  public ResponseEntity<ApiResponse<Map<String, String>>> previewStrategyCode(
      @PathVariable Long strategyId,
      @RequestParam(defaultValue = "1") Long memberId) {

    String pythonCode = strategyRunnerService.previewStrategyCode(strategyId, memberId);

    return ResponseEntity
        .ok()
        .body(ApiResponse.success(
            "STRATEGY_CODE_PREVIEW_SUCCESS",
            "전략 코드 미리보기 성공",
            Map.of(
                "strategyId", strategyId.toString(),
                "pythonCode", pythonCode)));
  }
}
