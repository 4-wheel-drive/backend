package com.pda.strategy_service.controller;

import com.pda.strategy_service.domain.mongodb.CustomStrategy;
import com.pda.strategy_service.repository.mongodb.CustomStrategyRepository;
import com.pda.strategy_service.service.StrategyCodeGenerator;
import com.pda.strategy_service.service.KubernetesPodManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 컨트롤러
 * MongoDB의 전략을 직접 실행
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestController {

  private final CustomStrategyRepository customStrategyRepository;
  private final StrategyCodeGenerator codeGenerator;
  private final KubernetesPodManager podManager;

  /**
   * MongoDB의 모든 전략 조회
   */
  @GetMapping("/strategies")
  public Map<String, Object> listStrategies() {
    List<CustomStrategy> strategies = customStrategyRepository.findAll();

    return Map.of(
        "count", strategies.size(),
        "strategies", strategies.stream()
            .map(s -> Map.of(
                "strategyId", s.getStrategyId() != null ? s.getStrategyId() : 0,
                "strategyName", s.getStrategyName(),
                "version", s.getVersion(),
                "createdAt", s.getCreatedAt()))
            .toList());
  }

  /**
   * MongoDB의 전략을 Python 코드로 변환 (미리보기)
   */
  @GetMapping("/strategies/{strategyId}/code")
  public Map<String, Object> generateCode(
      @PathVariable Long strategyId,
      @RequestParam(defaultValue = "1") int memberId,
      @RequestParam(defaultValue = "005930") String symbol) {

    CustomStrategy customStrategy = customStrategyRepository.findByStrategyId(strategyId);
    if (customStrategy == null) {
      return Map.of("error", "전략을 찾을 수 없습니다");
    }

    Map<String, Object> strategyJson = convertToMap(customStrategy);
    String pythonCode = codeGenerator.generateCode(strategyJson, memberId, symbol);

    return Map.of(
        "strategyId", strategyId,
        "strategyName", customStrategy.getStrategyName(),
        "symbol", symbol,
        "codeLength", pythonCode.length(),
        "code", pythonCode);
  }

  /**
   * MongoDB의 전략으로 직접 Pod 생성 (테스트)
   */
  @PostMapping("/strategies/{strategyId}/run")
  public Map<String, Object> runStrategy(
      @PathVariable Long strategyId,
      @RequestParam(defaultValue = "1") int memberId,
      @RequestParam(defaultValue = "005930") String symbol) {

    log.info("🧪 테스트: 전략 실행 - strategyId: {}, memberId: {}, symbol: {}", strategyId, memberId, symbol);

    CustomStrategy customStrategy = customStrategyRepository.findByStrategyId(strategyId);
    if (customStrategy == null) {
      return Map.of("error", "전략을 찾을 수 없습니다");
    }

    try {
      // 1. Python 코드 생성
      Map<String, Object> strategyJson = convertToMap(customStrategy);
      String pythonCode = codeGenerator.generateCode(strategyJson, memberId, symbol);
      log.info("✅ Python 코드 생성 완료 - {} bytes", pythonCode.length());

      // 2. Pod 생성
      String podName = podManager.createStrategyPod(
          Long.valueOf(strategyId),
          Long.valueOf(memberId),
          pythonCode,
          symbol);
      log.info("✅ Pod 생성 완료 - {}", podName);

      return Map.of(
          "success", true,
          "strategyId", strategyId,
          "podName", podName,
          "codeLength", pythonCode.length(),
          "message", "전략 Pod가 생성되었습니다");
    } catch (Exception e) {
      log.error("❌ 전략 실행 실패", e);
      return Map.of(
          "error", e.getMessage(),
          "strategyId", strategyId);
    }
  }

  /**
   * Pod 삭제 (테스트)
   */
  @DeleteMapping("/pods/{memberId}/{symbol}")
  public Map<String, Object> deletePod(
      @PathVariable int memberId,
      @PathVariable String symbol) {

    try {
      podManager.deleteStrategyPod(Long.valueOf(memberId), symbol);
      return Map.of(
          "success", true,
          "message", "Pod가 삭제되었습니다");
    } catch (Exception e) {
      return Map.of(
          "error", e.getMessage());
    }
  }

  private Map<String, Object> convertToMap(CustomStrategy customStrategy) {
    Map<String, Object> map = new HashMap<>();
    map.put("strategy_name", customStrategy.getStrategyName());
    map.put("version", customStrategy.getVersion());
    map.put("meta", customStrategy.getMeta());
    map.put("buy", customStrategy.getBuy());
    map.put("sell", customStrategy.getSell());
    return map;
  }
}

