# Strategy Service 배포 가이드

## 📋 목차

1. [개요](#개요)
2. [사전 준비](#사전-준비)
3. [빌드 및 푸시](#빌드-및-푸시)
4. [배포](#배포)
5. [검증](#검증)
6. [트러블슈팅](#트러블슈팅)

---

## 🎯 개요

Strategy Service를 AWS EKS 클러스터에 배포하는 전체 프로세스

### 아키텍처

```
Docker Build → ECR Push → EKS Deployment
     ↓              ↓            ↓
  Dockerfile    AWS ECR    Kubernetes Pod
```

### 주요 기능

- ✅ 전략 CRUD 관리
- ✅ MongoDB 연동
- ✅ OpenAI API 통합
- ✅ Kubernetes Pod 생성/관리 (전략 실행)
- ✅ Redis 캐싱

---

## 📦 사전 준비

### 1. AWS CLI 인증

```bash
aws configure
aws eks update-kubeconfig --name modular1 --region ap-northeast-2
```

### 2. kubectl 컨텍스트 확인

```bash
kubectl config current-context
# 출력: modular1@eks
```

### 3. Docker 실행 확인

```bash
docker info
```

### 4. 필요한 Secrets 생성

#### MongoDB Secret

```bash
kubectl create secret generic mongodb-secret \
  --from-literal=uri='mongodb+srv://username:password@cluster.mongodb.net/strategy-db' \
  -n backend
```

#### OpenAI Secret

```bash
kubectl create secret generic openai-secret \
  --from-literal=api-key='sk-proj-your-api-key' \
  -n backend
```

#### ECR Secret (자동 생성됨)

배포 스크립트가 자동으로 생성하지만, 수동으로 생성하려면:

```bash
AWS_REGION="ap-northeast-2"
AWS_ACCOUNT_ID="618221165332"

aws ecr get-login-password --region ${AWS_REGION} | \
  kubectl create secret docker-registry ecr-secret \
  --docker-server=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com \
  --docker-username=AWS \
  --docker-password="$(cat -)" \
  --namespace=backend
```

---

## 🔨 빌드 및 푸시

### 방법 1: 자동화 스크립트 사용 (권장)

```bash
# 프로젝트 루트에서 실행
./scripts/build-and-push-strategy-service.sh

# 특정 태그 지정
./scripts/build-and-push-strategy-service.sh v1.0.0
```

### 방법 2: 수동 빌드

#### Step 1: Docker 빌드

```bash
cd backend/strategy-service

docker build \
  --platform linux/amd64 \
  -t strategy-service:latest \
  -f Dockerfile \
  ..
```

#### Step 2: ECR 로그인

```bash
AWS_REGION="ap-northeast-2"
AWS_ACCOUNT_ID="618221165332"

aws ecr get-login-password --region ${AWS_REGION} | \
  docker login --username AWS --password-stdin \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
```

#### Step 3: 이미지 태깅

```bash
docker tag strategy-service:latest \
  618221165332.dkr.ecr.ap-northeast-2.amazonaws.com/strategy-service:latest
```

#### Step 4: ECR 푸시

```bash
docker push 618221165332.dkr.ecr.ap-northeast-2.amazonaws.com/strategy-service:latest
```

---

## 🚀 배포

### 방법 1: 자동화 스크립트 사용 (권장)

```bash
# 프로젝트 루트에서 실행
./scripts/deploy-strategy-service.sh
```

스크립트가 자동으로:

1. RBAC 적용
2. ECR Secret 확인/생성
3. Secrets 확인
4. Deployment 적용
5. 롤아웃 상태 확인

### 방법 2: 수동 배포

#### Step 1: RBAC 적용

```bash
kubectl apply -f manifests/backend/strategy-service-rbac.yaml
```

#### Step 2: Deployment 적용

```bash
kubectl apply -f manifests/backend/strategy-service.yaml
```

#### Step 3: 롤아웃 상태 확인

```bash
kubectl rollout status deployment/strategy-service -n backend
```

---

## ✅ 검증

### 1. Pod 상태 확인

```bash
kubectl get pods -n backend -l app=strategy-service

# 출력 예시:
# NAME                               READY   STATUS    RESTARTS   AGE
# strategy-service-7d9f8b5c4-abc12   1/1     Running   0          2m
# strategy-service-7d9f8b5c4-def34   1/1     Running   0          2m
```

### 2. Deployment 상태 확인

```bash
kubectl get deployment strategy-service -n backend

# 출력 예시:
# NAME               READY   UP-TO-DATE   AVAILABLE   AGE
# strategy-service   2/2     2            2           5m
```

### 3. Service 확인

```bash
kubectl get svc strategy-service -n backend

# 출력 예시:
# NAME               TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
# strategy-service   ClusterIP   10.100.200.50   <none>        8081/TCP   5m
```

### 4. 로그 확인

```bash
# 최신 Pod의 로그 확인
POD_NAME=$(kubectl get pods -n backend -l app=strategy-service -o jsonpath='{.items[0].metadata.name}')
kubectl logs -f ${POD_NAME} -n backend

# 모든 Pod의 로그 확인
kubectl logs -l app=strategy-service -n backend --tail=100
```

### 5. 헬스 체크

```bash
# Pod 내부에서 헬스체크
kubectl exec -it ${POD_NAME} -n backend -- wget -O- http://localhost:8081/actuator/health

# 또는 Port-forward로 로컬에서 확인
kubectl port-forward svc/strategy-service 8081:8081 -n backend
curl http://localhost:8081/actuator/health
```

### 6. 이벤트 확인

```bash
kubectl get events -n backend --sort-by='.lastTimestamp' | grep strategy-service
```

---

## 🔍 트러블슈팅

### ImagePullBackOff 에러

**증상**: Pod가 `ImagePullBackOff` 상태

**원인**: ECR 인증 실패

**해결**:

```bash
# ECR Secret 재생성
kubectl delete secret ecr-secret -n backend

aws ecr get-login-password --region ap-northeast-2 | \
  kubectl create secret docker-registry ecr-secret \
  --docker-server=618221165332.dkr.ecr.ap-northeast-2.amazonaws.com \
  --docker-username=AWS \
  --docker-password="$(cat -)" \
  --namespace=backend

# Deployment 재시작
kubectl rollout restart deployment/strategy-service -n backend
```

### CrashLoopBackOff 에러

**증상**: Pod가 계속 재시작

**원인**: 애플리케이션 시작 실패

**해결**:

```bash
# 로그 확인
kubectl logs ${POD_NAME} -n backend --previous

# 일반적인 원인:
# 1. MongoDB 연결 실패 → Secret 확인
# 2. Redis 연결 실패 → Redis Pod 상태 확인
# 3. 포트 충돌 → Deployment YAML 확인
```

### Pending 상태

**증상**: Pod가 `Pending` 상태에서 멈춤

**원인**: 리소스 부족 또는 노드 스케줄링 실패

**해결**:

```bash
# Pod 이벤트 확인
kubectl describe pod ${POD_NAME} -n backend

# 노드 리소스 확인
kubectl top nodes

# 필요 시 리소스 요청량 조정
kubectl edit deployment strategy-service -n backend
```

### MongoDB 연결 실패

**증상**: 로그에 MongoDB 연결 에러

**해결**:

```bash
# Secret 확인
kubectl get secret mongodb-secret -n backend -o yaml

# URI 디코딩 및 확인
kubectl get secret mongodb-secret -n backend -o jsonpath='{.data.uri}' | base64 -d

# Secret 재생성
kubectl delete secret mongodb-secret -n backend
kubectl create secret generic mongodb-secret \
  --from-literal=uri='mongodb+srv://correct-uri' \
  -n backend

# Deployment 재시작
kubectl rollout restart deployment/strategy-service -n backend
```

### Kubernetes API 권한 에러

**증상**: 전략 실행 시 Pod 생성 권한 에러

**해결**:

```bash
# RBAC 확인
kubectl get sa strategy-service -n backend
kubectl get role strategy-pod-manager -n backend
kubectl get rolebinding strategy-service-binding -n backend

# RBAC 재적용
kubectl apply -f manifests/backend/strategy-service-rbac.yaml

# Deployment 재시작
kubectl rollout restart deployment/strategy-service -n backend
```

---

## 📊 모니터링

### 리소스 사용량

```bash
# Pod 리소스 사용량
kubectl top pods -n backend -l app=strategy-service

# 노드별 리소스
kubectl top nodes
```

### HPA 상태

```bash
# HorizontalPodAutoscaler 확인
kubectl get hpa strategy-service-hpa -n backend

# 상세 정보
kubectl describe hpa strategy-service-hpa -n backend
```

### Metrics

```bash
# Actuator metrics 확인
kubectl port-forward svc/strategy-service 8081:8081 -n backend
curl http://localhost:8081/actuator/metrics
```

---

## 🔄 업데이트

### 이미지 업데이트

```bash
# 1. 새 이미지 빌드 및 푸시
./scripts/build-and-push-strategy-service.sh v1.1.0

# 2. Deployment 이미지 업데이트
kubectl set image deployment/strategy-service \
  strategy-service=618221165332.dkr.ecr.ap-northeast-2.amazonaws.com/strategy-service:v1.1.0 \
  -n backend

# 3. 롤아웃 상태 확인
kubectl rollout status deployment/strategy-service -n backend
```

### 롤백

```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/strategy-service -n backend

# 특정 리비전으로 롤백
kubectl rollout history deployment/strategy-service -n backend
kubectl rollout undo deployment/strategy-service --to-revision=2 -n backend
```

---

## 🔗 관련 문서

- [EKS 설정 가이드](./EKS_SETUP_GUIDE.md)
- [전략 실행 시스템](../../manifests/strategy/strategy-pod-example.yaml)
- [RBAC 설정](../../manifests/backend/strategy-service-rbac.yaml)

---

## 📞 지원

문제 발생 시 다음 정보를 포함하여 문의:

1. Pod 이름 및 상태
2. `kubectl describe pod` 출력
3. `kubectl logs` 출력
4. 실행한 명령어 및 에러 메시지

