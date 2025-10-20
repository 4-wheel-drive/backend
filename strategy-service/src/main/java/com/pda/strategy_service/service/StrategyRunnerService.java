package com.pda.strategy_service.service;

/**
 * 전략 실행 관리 서비스
 * EKS 환경에서 전략 Pod 생성/중지를 담당
 */
public interface StrategyRunnerService {

  /**
   * 전략 실행 시작
   * 
   * @param strategyId 전략 ID
   * @param memberId   회원 ID
   * @return 생성된 Pod 이름
   */
  String strategyStart(Long strategyId, Long memberId);

  /**
   * 전략 실행 중지
   * 
   * @param strategyId 전략 ID
   * @param memberId   회원 ID
   */
  void strategyStop(Long strategyId, Long memberId);

  /**
   * 전략 Pod 실행 상태 확인
   * 
   * @param strategyId 전략 ID
   * @param memberId   회원 ID
   * @return Pod가 실행 중이면 true
   */
  boolean isStrategyRunning(Long strategyId, Long memberId);

  /**
   * 전략 파이썬 코드 미리보기 (개발용)
   * Pod를 생성하지 않고 생성될 파이썬 코드만 반환
   * 
   * @param strategyId 전략 ID
   * @param memberId   회원 ID
   * @return 생성된 파이썬 코드
   */
  String previewStrategyCode(Long strategyId, Long memberId);
}
