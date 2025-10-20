package com.pda.strategy_service.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes Pod 관리 서비스
 * EKS 환경에서 전략 실행을 위한 Pod 생성/삭제를 담당
 */
@Slf4j
@Service
public class KubernetesPodManager {

  private CoreV1Api coreV1Api;

  // EKS 설정
  @Value("${kubernetes.namespace:backend}")
  private String namespace;

  @Value("${kubernetes.ecr-image:618221165332.dkr.ecr.ap-northeast-2.amazonaws.com/strategy-runner:latest}")
  private String ecrImage;

  @Value("${kubernetes.image-pull-secret:ecr-secret}")
  private String imagePullSecret;

  // 환경 변수 설정
  @Value("${kafka.bootstrap-servers:my-cluster-kafka-bootstrap.kafka:9092}")
  private String kafkaBrokers;

  @Value("${spring.data.redis.host:redis-master.redis}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private String redisPort;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Value("${trading-service.url:http://trading-service.backend:8082}")
  private String tradingServiceUrl;

  @Value("${strategy-service.url:http://strategy-service.backend:8081}")
  private String strategyServiceUrl;

  @PostConstruct
  public void init() {
    try {
      // EKS 클러스터 내부에서 실행 시 자동으로 인증 정보 로드
      ApiClient client = Config.defaultClient();
      Configuration.setDefaultApiClient(client);
      this.coreV1Api = new CoreV1Api(client);
      log.info("✅ Kubernetes API Client 초기화 완료");
    } catch (IOException e) {
      log.error("❌ Kubernetes API Client 초기화 실패", e);
      // 로컬 개발 환경에서는 무시
    }
  }

  /**
   * 전략 실행 Pod 생성
   * 
   * @param strategyId   전략 ID
   * @param memberId     회원 ID
   * @param pythonCode   실행할 Python 코드
   * @param symbol       종목 코드
   * @param strategyJson 전략 JSON (buy/sell 정보 포함)
   * @return Pod 이름
   */
  public String createStrategyPod(Long strategyId, Long memberId, String pythonCode, String symbol,
      Map<String, Object> strategyJson) {
    log.info("전략 Pod 생성 시작 - strategyId: {}, memberId: {}, symbol: {}", strategyId, memberId, symbol);

    String podName = generatePodName(memberId, symbol, strategyJson);
    String configMapName = generateConfigMapName(memberId, symbol, strategyJson);

    try {
      // 1. 기존 리소스 정리 (있다면)
      cleanupExistingResources(podName, configMapName);

      // 2. ConfigMap 생성 (Python 코드 저장)
      createConfigMap(configMapName, pythonCode, strategyId, memberId, symbol);

      // 3. Pod 생성
      createPod(podName, configMapName, strategyId, memberId, symbol);

      log.info("전략 Pod 생성 완료 - podName: {}", podName);
      return podName;
    } catch (Exception e) {
      log.error("전략 Pod 생성 실패 - strategyId: {}, error: {}", strategyId, e.getMessage(), e);
      throw new RuntimeException("전략 Pod 생성 실패: " + e.getMessage(), e);
    }
  }

  /**
   * 전략 실행 Pod 삭제
   * 
   * @param memberId     회원 ID
   * @param symbol       종목 코드
   * @param strategyJson 전략 JSON (buy/sell 정보 포함)
   */
  public void deleteStrategyPod(Long memberId, String symbol, Map<String, Object> strategyJson) {
    log.info("전략 Pod 삭제 시작 - memberId: {}, symbol: {}", memberId, symbol);

    String podName = generatePodName(memberId, symbol, strategyJson);
    String configMapName = generateConfigMapName(memberId, symbol, strategyJson);

    try {
      cleanupExistingResources(podName, configMapName);
      log.info("전략 Pod 삭제 완료 - podName: {}", podName);
    } catch (Exception e) {
      log.error("전략 Pod 삭제 실패 - memberId: {}, symbol: {}, error: {}", memberId, symbol, e.getMessage(), e);
      throw new RuntimeException("전략 Pod 삭제 실패: " + e.getMessage(), e);
    }
  }

  /**
   * ConfigMap 생성
   */
  private void createConfigMap(String configMapName, String pythonCode, Long strategyId, Long memberId, String symbol) {
    if (coreV1Api == null) {
      log.warn("Kubernetes API 사용 불가 - ConfigMap 생성 생략");
      return;
    }

    log.info("ConfigMap 생성 - name: {}", configMapName);

    V1ConfigMap configMap = new V1ConfigMap();

    // Metadata 설정
    V1ObjectMeta metadata = new V1ObjectMeta();
    metadata.setName(configMapName);
    metadata.setNamespace(namespace);
    metadata.setLabels(Map.of(
        "app", "strategy-executor",
        "strategy-id", String.valueOf(strategyId),
        "member-id", String.valueOf(memberId),
        "symbol", symbol));
    configMap.setMetadata(metadata);

    // Data 설정
    configMap.setData(Map.of("strategy.py", pythonCode));

    try {
      coreV1Api.createNamespacedConfigMap(namespace, configMap).execute();
      log.info("ConfigMap 생성 완료 - name: {}", configMapName);
    } catch (ApiException e) {
      log.error("ConfigMap 생성 실패 - code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
      throw new RuntimeException("ConfigMap 생성 실패", e);
    }
  }

  /**
   * Pod 생성
   */
  private void createPod(String podName, String configMapName, Long strategyId, Long memberId, String symbol) {
    if (coreV1Api == null) {
      log.warn("Kubernetes API 사용 불가 - Pod 생성 생략");
      return;
    }

    log.info("Pod 생성 - name: {}", podName);

    V1Pod pod = new V1Pod();

    // Metadata 설정
    V1ObjectMeta metadata = new V1ObjectMeta();
    metadata.setName(podName);
    metadata.setNamespace(namespace);
    metadata.setLabels(Map.of(
        "app", "strategy-executor",
        "strategy-id", String.valueOf(strategyId),
        "member-id", String.valueOf(memberId),
        "symbol", symbol));
    pod.setMetadata(metadata);

    // PodSpec 설정
    V1PodSpec spec = new V1PodSpec();

    // ImagePullSecrets 설정 (ECR 인증)
    V1LocalObjectReference pullSecret = new V1LocalObjectReference();
    pullSecret.setName(imagePullSecret);
    spec.setImagePullSecrets(List.of(pullSecret));

    // Container 설정
    V1Container container = new V1Container();
    container.setName("strategy-runner");
    container.setImage(ecrImage);
    container.setCommand(List.of("python", "-u", "/app/strategy.py")); // -u: unbuffered output
    container.setImagePullPolicy("Always");

    // 환경 변수 설정
    List<V1EnvVar> envVars = List.of(
        createEnvVar("STRATEGY_ID", String.valueOf(strategyId)),
        createEnvVar("MEMBER_ID", String.valueOf(memberId)),
        createEnvVar("SYMBOL", symbol),
        createEnvVar("KAFKA_BROKERS", kafkaBrokers),
        createEnvVar("REDIS_HOST", redisHost),
        createEnvVar("REDIS_PORT", redisPort),
        createEnvVar("REDIS_PASSWORD", redisPassword),
        createEnvVar("TRADING_SERVICE_URL", tradingServiceUrl),
        createEnvVar("STRATEGY_SERVICE_URL", strategyServiceUrl));
    container.setEnv(envVars);

    // Volume Mount 설정
    V1VolumeMount volumeMount = new V1VolumeMount();
    volumeMount.setName("strategy-code");
    volumeMount.setMountPath("/app/strategy.py");
    volumeMount.setSubPath("strategy.py");
    container.setVolumeMounts(List.of(volumeMount));

    // 리소스 제한 설정 (최적화: 실제 사용량 기반)
    V1ResourceRequirements resources = new V1ResourceRequirements();
    resources.setRequests(Map.of(
        "memory", io.kubernetes.client.custom.Quantity.fromString("128Mi"),
        "cpu", io.kubernetes.client.custom.Quantity.fromString("50m")));
    resources.setLimits(Map.of(
        "memory", io.kubernetes.client.custom.Quantity.fromString("256Mi"),
        "cpu", io.kubernetes.client.custom.Quantity.fromString("100m")));
    container.setResources(resources);

    spec.setContainers(List.of(container));

    // Volume 설정 (ConfigMap)
    V1Volume volume = new V1Volume();
    volume.setName("strategy-code");
    V1ConfigMapVolumeSource configMapSource = new V1ConfigMapVolumeSource();
    configMapSource.setName(configMapName);
    volume.setConfigMap(configMapSource);
    spec.setVolumes(List.of(volume));

    // 재시작 정책 설정 (전략 실행은 한 번만)
    spec.setRestartPolicy("Never");

    // RuntimeClass 지정 (VPC webhook overhead 이슈 해결)
    spec.setRuntimeClassName("runc");

    // API version 설정
    pod.setApiVersion("v1");
    pod.setKind("Pod");
    pod.setSpec(spec);

    try {
      coreV1Api.createNamespacedPod(namespace, pod).execute();
      log.info("Pod 생성 완료 - name: {}", podName);
    } catch (ApiException e) {
      log.error("Pod 생성 실패 - code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
      throw new RuntimeException("Pod 생성 실패", e);
    }
  }

  /**
   * 환경 변수 생성 헬퍼 메서드
   */
  private V1EnvVar createEnvVar(String name, String value) {
    V1EnvVar envVar = new V1EnvVar();
    envVar.setName(name);
    envVar.setValue(value);
    return envVar;
  }

  /**
   * 기존 리소스 정리
   */
  private void cleanupExistingResources(String podName, String configMapName) {
    if (coreV1Api == null) {
      return;
    }

    // Pod 삭제
    try {
      coreV1Api.deleteNamespacedPod(podName, namespace).execute();
      log.info("기존 Pod 삭제 완료 - name: {}", podName);
      // Pod 삭제 대기 (최대 5초)
      Thread.sleep(5000);
    } catch (ApiException e) {
      if (e.getCode() != 404) {
        log.warn("Pod 삭제 중 오류 - code: {}", e.getCode());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // ConfigMap 삭제
    try {
      coreV1Api.deleteNamespacedConfigMap(configMapName, namespace).execute();
      log.info("기존 ConfigMap 삭제 완료 - name: {}", configMapName);
    } catch (ApiException e) {
      if (e.getCode() != 404) {
        log.warn("ConfigMap 삭제 중 오류 - code: {}", e.getCode());
      }
    }
  }

  /**
   * Pod 상태 확인
   */
  public boolean isPodRunning(Long memberId, String symbol, Map<String, Object> strategyJson) {
    if (coreV1Api == null) {
      log.warn("Kubernetes API 사용 불가 - Pod 상태 확인 불가");
      return false;
    }

    String podName = generatePodName(memberId, symbol, strategyJson);

    try {
      V1Pod pod = coreV1Api.readNamespacedPod(podName, namespace).execute();
      String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";
      log.info("Pod 상태 확인 - name: {}, phase: {}", podName, phase);
      return "Running".equals(phase);
    } catch (ApiException e) {
      if (e.getCode() == 404) {
        log.info("Pod 없음 - name: {}", podName);
        return false;
      }
      log.error("Pod 상태 확인 실패 - name: {}, code: {}", podName, e.getCode(), e);
      return false;
    }
  }

  /**
   * Pod 이름 생성: strategy-{memberId}-{symbol}-{type}
   * type: buy (매수만), sell (매도만), 또는 빈 문자열 (둘 다)
   */
  private String generatePodName(Long memberId, String symbol, Map<String, Object> strategyJson) {
    String suffix = determineSuffix(strategyJson);
    if (suffix.isEmpty()) {
      return String.format("strategy-%d-%s", memberId, symbol.toLowerCase());
    }
    return String.format("strategy-%d-%s-%s", memberId, symbol.toLowerCase(), suffix);
  }

  /**
   * ConfigMap 이름 생성: strategy-code-{memberId}-{symbol}-{type}
   */
  private String generateConfigMapName(Long memberId, String symbol, Map<String, Object> strategyJson) {
    String suffix = determineSuffix(strategyJson);
    if (suffix.isEmpty()) {
      return String.format("strategy-code-%d-%s", memberId, symbol.toLowerCase());
    }
    return String.format("strategy-code-%d-%s-%s", memberId, symbol.toLowerCase(), suffix);
  }

  /**
   * 전략 JSON에서 buy/sell 여부를 확인하여 suffix 결정
   * - buy만 있으면: "buy"
   * - sell만 있으면: "sell"
   * - 둘 다 있거나 없으면: ""
   */
  private String determineSuffix(Map<String, Object> strategyJson) {
    if (strategyJson == null) {
      return "";
    }

    boolean hasBuy = strategyJson.containsKey("buy") && strategyJson.get("buy") != null;
    boolean hasSell = strategyJson.containsKey("sell") && strategyJson.get("sell") != null;

    if (hasBuy && !hasSell) {
      return "buy";
    } else if (!hasBuy && hasSell) {
      return "sell";
    }
    return ""; // 둘 다 있거나 둘 다 없으면 suffix 없음
  }
}
