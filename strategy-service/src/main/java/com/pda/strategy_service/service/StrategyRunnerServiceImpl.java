package com.pda.strategy_service.service;

import com.pda.common_service.exception.AuthException;
import com.pda.common_service.exception.ResourceNotFound;
import com.pda.common_service.exception.StrategyException;
import com.pda.common_service.response.ResponseMessage;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.StrategyActivatedStatus;
import com.pda.strategy_service.domain.StrategyExistedStatus;
import com.pda.strategy_service.domain.mongodb.CustomStrategy;
import com.pda.strategy_service.repository.jpa.StrategyRepository;
import com.pda.strategy_service.repository.mongodb.CustomStrategyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 전략 실행 관리 서비스 구현체
 * EKS 환경에서 전략 Pod를 생성하고 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyRunnerServiceImpl implements StrategyRunnerService {

  private final StrategyCodeGenerator codeGenerator;
  private final KubernetesPodManager podManager;
  private final CustomStrategyRepository customStrategyRepository;
  private final StrategyRepository strategyRepository;

  /**
   * 전략 실행 시작
   * 1. 전략 검증
   * 2. Python 코드 생성
   * 3. Kubernetes Pod 생성
   * 4. 전략 상태 변경
   */
  @Override
  @Transactional
  public String strategyStart(Long strategyId, Long memberId) {
    log.info("🚀 전략 실행 시작 요청 - strategyId: {}, memberId: {}", strategyId, memberId);

    // 1. 전략 조회 및 권한 확인
    Strategy strategy = validateStrategyAccess(strategyId, memberId);

    // 2. 이미 실행 중인지 확인
    if (strategy.getStrategyActivatedStatus() == StrategyActivatedStatus.ACTIVATED) {
      log.warn("⚠️ 전략이 이미 실행 중입니다 - strategyId: {}", strategyId);
      throw new StrategyException(ResponseMessage.STRATEGY_SAVE_FAILED);
    }

    // 3. MongoDB에서 전략 상세 정보 조회
    CustomStrategy customStrategy = customStrategyRepository.findByStrategyId(strategyId);
    if (customStrategy == null) {
      log.error("❌ 전략 상세 정보를 찾을 수 없습니다 - strategyId: {}", strategyId);
      throw new ResourceNotFound(ResponseMessage.STRATEGY_NOT_FOUND);
    }

    // 4. CustomStrategy를 Map으로 변환
    Map<String, Object> strategyJson = convertToStrategyJson(customStrategy);

    // 5. 종목 코드 추출
    String symbol = extractSymbol(customStrategy, strategy);

    // 6. StrategyCodeGenerator를 사용하여 Python 코드 생성
    log.info("📝 Python 코드 생성 시작 - strategyId: {}, symbol: {}", strategyId, symbol);
    String pythonCode = codeGenerator.generateCode(
        strategyJson,
        memberId.intValue(),
        symbol);
    log.info("✅ Python 코드 생성 완료 - 길이: {} bytes", pythonCode.length());

    // 7. Kubernetes Pod 생성하여 전략 실행
    log.info("☸️ Kubernetes Pod 생성 시작 - strategyId: {}", strategyId);
    String podName = podManager.createStrategyPod(
        strategyId,
        memberId,
        pythonCode,
        symbol);

    // 8. 전략 상태를 ACTIVATED로 변경
    strategy.update(
        strategy.getStock(),
        strategy.getStrategyName(),
        StrategyActivatedStatus.ACTIVATED,
        strategy.getStrategyExistedStatus(),
        strategy.getStrategyProfitSummary());
    strategyRepository.save(strategy);

    log.info("🎉 전략 실행 시작 완료 - strategyId: {}, podName: {}", strategyId, podName);
    return podName;
  }

  /**
   * 전략 실행 중지
   * 1. 전략 검증
   * 2. Kubernetes Pod 삭제
   * 3. 전략 상태 변경
   */
  @Override
  @Transactional
  public void strategyStop(Long strategyId, Long memberId) {
    log.info("⏹️ 전략 실행 중지 요청 - strategyId: {}, memberId: {}", strategyId, memberId);

    // 1. 전략 조회 및 권한 확인
    Strategy strategy = validateStrategyAccess(strategyId, memberId);

    // 2. 실행 중이 아닌지 확인
    if (strategy.getStrategyActivatedStatus() == StrategyActivatedStatus.PENDING) {
      log.warn("⚠️ 전략이 실행 중이 아닙니다 - strategyId: {}", strategyId);
      throw new StrategyException(ResponseMessage.STRATEGY_NOT_FOUND);
    }

    // 3. 종목 코드 추출 (Pod 이름 생성에 필요)
    CustomStrategy customStrategy = customStrategyRepository.findByStrategyId(strategyId);
    String symbol = extractSymbol(customStrategy, strategy);

    // 4. Kubernetes Pod 삭제
    log.info("☸️ Kubernetes Pod 삭제 시작 - strategyId: {}", strategyId);
    podManager.deleteStrategyPod(memberId, symbol);

    // 5. 전략 상태를 PENDING으로 변경
    strategy.update(
        strategy.getStock(),
        strategy.getStrategyName(),
        StrategyActivatedStatus.PENDING,
        strategy.getStrategyExistedStatus(),
        strategy.getStrategyProfitSummary());
    strategyRepository.save(strategy);

    log.info("✅ 전략 실행 중지 완료 - strategyId: {}", strategyId);
  }

  /**
   * 전략 Pod 실행 상태 확인
   */
  @Override
  public boolean isStrategyRunning(Long strategyId, Long memberId) {
    log.info("🔍 전략 실행 상태 확인 - strategyId: {}, memberId: {}", strategyId, memberId);

    Strategy strategy = strategyRepository.findByIdAndStrategyExistedStatus(
        strategyId,
        StrategyExistedStatus.EXISTED).orElseThrow(() -> new ResourceNotFound(ResponseMessage.STRATEGY_NOT_FOUND));

    CustomStrategy customStrategy = customStrategyRepository.findByStrategyId(strategyId);
    String symbol = extractSymbol(customStrategy, strategy);

    boolean isRunning = podManager.isPodRunning(memberId, symbol);
    log.info("Pod 실행 상태 - strategyId: {}, running: {}", strategyId, isRunning);

    return isRunning;
  }

  /**
   * 전략 파이썬 코드 미리보기 (개발용)
   * Pod를 생성하지 않고 생성될 파이썬 코드만 반환
   */
  @Override
  public String previewStrategyCode(Long strategyId, Long memberId) {
    log.info("👀 전략 코드 미리보기 요청 - strategyId: {}, memberId: {}", strategyId, memberId);

    // 1. 전략 조회 및 권한 확인
    Strategy strategy = validateStrategyAccess(strategyId, memberId);

    // 2. MongoDB에서 전략 상세 정보 조회
    CustomStrategy customStrategy = customStrategyRepository.findByStrategyId(strategyId);
    if (customStrategy == null) {
      log.error("❌ 전략 상세 정보를 찾을 수 없습니다 - strategyId: {}", strategyId);
      throw new ResourceNotFound(ResponseMessage.STRATEGY_NOT_FOUND);
    }

    // 3. CustomStrategy를 Map으로 변환
    Map<String, Object> strategyJson = convertToStrategyJson(customStrategy);

    // 4. 종목 코드 추출
    String symbol = extractSymbol(customStrategy, strategy);

    // 5. StrategyCodeGenerator를 사용하여 Python 코드 생성
    log.info("📝 Python 코드 생성 시작 (미리보기) - strategyId: {}, symbol: {}", strategyId, symbol);
    String pythonCode = codeGenerator.generateCode(
        strategyJson,
        memberId.intValue(),
        symbol);
    log.info("✅ Python 코드 생성 완료 (미리보기) - 길이: {} bytes", pythonCode.length());

    return pythonCode;
  }

  /**
   * 전략 조회 및 권한 검증
   */
  private Strategy validateStrategyAccess(Long strategyId, Long memberId) {
    Strategy strategy = strategyRepository.findByIdAndStrategyExistedStatus(
        strategyId,
        StrategyExistedStatus.EXISTED).orElseThrow(() -> new ResourceNotFound(ResponseMessage.STRATEGY_NOT_FOUND));

    if (!Objects.equals(strategy.getMember().getId(), memberId)) {
      log.warn("🚫 전략 접근 권한 없음 - strategyId: {}, memberId: {}", strategyId, memberId);
      throw new AuthException(ResponseMessage.PERMISSION_DENY);
    }

    return strategy;
  }

  /**
   * CustomStrategy를 전략 JSON Map으로 변환
   */
  private Map<String, Object> convertToStrategyJson(CustomStrategy customStrategy) {
    Map<String, Object> strategyJson = new HashMap<>();
    strategyJson.put("strategy_name", customStrategy.getStrategyName());
    strategyJson.put("version", customStrategy.getVersion());
    strategyJson.put("meta", customStrategy.getMeta());
    strategyJson.put("buy", customStrategy.getBuy());
    strategyJson.put("sell", customStrategy.getSell());
    return strategyJson;
  }

  /**
   * 종목 코드 추출
   * 우선순위: CustomStrategy meta > Strategy Stock > 기본값(005930)
   */
  private String extractSymbol(CustomStrategy customStrategy, Strategy strategy) {
    // 1. CustomStrategy의 meta에서 universe 추출 시도
    if (customStrategy != null && customStrategy.getMeta() != null) {
      @SuppressWarnings("unchecked")
      List<String> universe = (List<String>) customStrategy.getMeta().get("universe");
      if (universe != null && !universe.isEmpty()) {
        return universe.get(0);
      }
    }

    // 2. Strategy의 Stock에서 종목 코드 추출
    if (strategy.getStock() != null) {
      String stockCode = strategy.getStock().toDto().stockCode();
      if (stockCode != null && !stockCode.isEmpty()) {
        return stockCode;
      }
    }

    // 3. 기본값 (삼성전자)
    log.warn("⚠️ 종목 코드를 찾을 수 없어 기본값 사용 - strategyId: {}", strategy.getId());
    return "005930";
  }
}
