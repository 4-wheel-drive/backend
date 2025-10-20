# Backend Services Deployment Guide

이 가이드는 멀티모듈 백엔드 서비스들을 Kubernetes 파드로 배포하는 방법을 설명합니다.

## 📦 서비스 구성

### 실행 서비스

- **auth-service** (포트 8080) - 인증 서비스
- **trading-service** (포트 8082) - 거래 서비스
- **strategy-service** (포트 8081) - 전략 서비스

### 공통 라이브러리

- **common-service** - 공통 기능 라이브러리 (다른 서비스들이 의존)

## 🚀 빠른 시작

### 전체 배포 프로세스 (처음부터 끝까지)

```bash
# 1. 이미지 빌드 및 ECR 푸시
cd backend
./scripts/build-and-push-amd64.sh

# 2. 서비스 배포
cd ../manifests/backend
kubectl apply -f auth-service.yaml
kubectl apply -f trading-service.yaml
kubectl apply -f strategy-service.yaml

# 3. 배포 상태 확인
kubectl get pods -n backend
kubectl rollout status deployment auth-service -n backend
kubectl rollout status deployment trading-service -n backend
kubectl rollout status deployment strategy-service -n backend
```

### 1단계: Docker 이미지 빌드 및 ECR 푸시

```bash
cd backend
./scripts/build-and-push-amd64.sh
```

이 스크립트는:

- ECR에 로그인
- 전체 프로젝트를 Gradle로 빌드
- 각 서비스별 Docker 이미지 생성 (AMD64 플랫폼)
- ECR에 이미지 푸시
- 빌드 결과 요약 출력

**개별 서비스만 빌드/푸시:**

```bash
# Auth Service만
./scripts/build-and-push-auth.sh

# Trading Service만
./scripts/build-and-push-trading.sh

# Strategy Service만
./scripts/build-and-push-strategy.sh
```

### 2단계: Kubernetes에 배포

```bash
# Secret 설정 (최초 1회만)
cd ../manifests/backend
# MariaDB, MongoDB, Redis 등 필요한 Secret이 이미 생성되어 있는지 확인
kubectl get secret -n backend

# 서비스 배포
kubectl apply -f auth-service.yaml
kubectl apply -f trading-service.yaml
kubectl apply -f strategy-service.yaml
```

### 3단계: 서비스 상태 확인

```bash
# Pod 상태 확인
kubectl get pods -n backend

# Service 상태 확인
kubectl get svc -n backend

# 배포 상태 확인
kubectl rollout status deployment auth-service -n backend
kubectl rollout status deployment trading-service -n backend
kubectl rollout status deployment strategy-service -n backend
```

## 📋 개별 명령어

### 네임스페이스 생성

```bash
kubectl create namespace backend
```

### 개별 서비스 배포

```bash
# auth-service 배포
kubectl apply -f manifests/backend/auth-service.yaml

# trading-service 배포
kubectl apply -f manifests/backend/trading-service.yaml

# strategy-service 배포
kubectl apply -f manifests/backend/strategy-service.yaml
```

### Pod 상태 확인

```bash
# 전체 Pod 조회
kubectl get pods -n backend

# 실시간 모니터링
kubectl get pods -n backend -w

# 특정 서비스의 Pod 조회
kubectl get pods -n backend -l app=auth-service
```

### Service 상태 확인

```bash
kubectl get svc -n backend
```

### 로그 확인

```bash
# 특정 서비스의 로그 확인
kubectl logs -n backend -l app=auth-service --tail=50

# 실시간 로그 확인
kubectl logs -n backend -l app=auth-service -f

# 특정 Pod 로그 확인
kubectl logs -n backend <pod-name>
```

### Pod 상세 정보

```bash
kubectl describe pod -n backend <pod-name>
```

### Pod 내부 접속

```bash
kubectl exec -it -n backend <pod-name> -- /bin/sh
```

### 포트 포워딩

```bash
# auth-service 로컬 접속
kubectl port-forward -n backend svc/auth-service 8080:8080

# strategy-service 로컬 접속
kubectl port-forward -n backend svc/strategy-service 8081:8081

# trading-service 로컬 접속
kubectl port-forward -n backend svc/trading-service 8082:8082
```

### 서비스 재시작 (이미지 업데이트 후)

```bash
# 개별 재시작
kubectl rollout restart deployment auth-service -n backend
kubectl rollout restart deployment trading-service -n backend
kubectl rollout restart deployment strategy-service -n backend

# 또는 한번에
kubectl rollout restart deployment auth-service trading-service strategy-service -n backend
```

### 전체 서비스 삭제

```bash
kubectl delete -f manifests/backend/auth-service.yaml
kubectl delete -f manifests/backend/trading-service.yaml
kubectl delete -f manifests/backend/strategy-service.yaml
```

## 🔧 문제 해결

### Pod가 Running 상태가 아닐 때

```bash
# Pod 상세 정보 확인
kubectl describe pod -n backend <pod-name>

# 최근 이벤트 확인
kubectl get events -n backend --sort-by='.lastTimestamp'

# 로그 확인
kubectl logs -n backend <pod-name>
```

### 이미지를 찾을 수 없다는 에러

```bash
# ECR 이미지 확인 (AWS CLI 필요)
aws ecr describe-images --repository-name auth-service --region ap-northeast-2
aws ecr describe-images --repository-name trading-service --region ap-northeast-2
aws ecr describe-images --repository-name strategy-service --region ap-northeast-2

# 이미지 다시 빌드 및 푸시
cd backend
./scripts/build-and-push-amd64.sh
```

### Pod가 재시작을 반복할 때

```bash
# 재시작 횟수 확인
kubectl get pods -n backend

# 이전 로그 확인
kubectl logs -n backend <pod-name> --previous

# 리소스 제한 확인
kubectl describe pod -n backend <pod-name>
```

### 헬스체크 실패

```bash
# Pod 내부에서 헬스체크 엔드포인트 확인
kubectl exec -n backend <pod-name> -- wget -q -O- http://localhost:8080/actuator/health

# 애플리케이션 로그 확인
kubectl logs -n backend <pod-name> --tail=100
```

## 📁 파일 구조

```
backend/
├── auth-service/
│   ├── Dockerfile
│   └── src/...
├── trading-service/
│   ├── Dockerfile
│   └── src/...
├── strategy-service/
│   ├── Dockerfile
│   └── src/...
├── common-service/
│   └── src/...
├── scripts/
│   ├── build-and-push-amd64.sh      # 3개 서비스 모두 빌드/푸시
│   ├── build-and-push-auth.sh       # Auth Service만
│   ├── build-and-push-trading.sh    # Trading Service만
│   └── build-and-push-strategy.sh   # Strategy Service만
├── build.gradle
└── settings.gradle

manifests/backend/
├── auth-service.yaml
├── trading-service.yaml
├── strategy-service.yaml
├── mariadb-secret-example.yaml
└── strategy-service-secrets-example.yaml
```

## 🔐 시크릿 설정

### 필수 시크릿

#### 1. MariaDB 연결 정보 (모든 서비스)

```bash
kubectl create secret generic mariadb-secret \
  --from-literal=url='jdbc:mariadb://YOUR_RDS_ENDPOINT:3306/DATABASE' \
  --from-literal=username='YOUR_USERNAME' \
  --from-literal=password='YOUR_PASSWORD' \
  -n backend
```

#### 2. MongoDB 연결 정보 (strategy-service)

```bash
kubectl create secret generic mongodb-secret \
  --from-literal=uri='mongodb+srv://USERNAME:PASSWORD@CLUSTER/DATABASE' \
  -n backend
```

#### 3. Redis Password

```bash
# Redis Helm 차트 설치 시 자동 생성됨
# 또는 수동 생성:
kubectl create secret generic redis \
  --from-literal=redis-password='YOUR_REDIS_PASSWORD' \
  -n backend
```

### 선택적 시크릿

#### 4. KIS API 키 (auth-service, optional)

```bash
kubectl create secret generic kis-secret \
  --from-literal=appkey='YOUR_KIS_APPKEY' \
  --from-literal=appsecret='YOUR_KIS_APPSECRET' \
  -n backend
```

#### 5. OpenAI API 키 (strategy-service, optional)

```bash
kubectl create secret generic openai-secret \
  --from-literal=api-key='YOUR_OPENAI_API_KEY' \
  -n backend
```

### 시크릿 확인

```bash
# 모든 시크릿 목록 확인
kubectl get secret -n backend

# 특정 시크릿 상세 정보
kubectl describe secret mariadb-secret -n backend
```

## 🌐 서비스 엔드포인트

각 서비스는 ClusterIP 타입으로 배포되어 클러스터 내부에서만 접근 가능합니다.

- `auth-service.backend:8080` - 인증 서비스
- `trading-service.backend:8082` - 거래 서비스
- `strategy-service.backend:8081` - 전략 서비스

외부에서 접근하려면 포트 포워딩을 사용하거나 Ingress를 설정하세요.

## 📊 리소스 사용량

각 서비스의 기본 리소스 설정:

- **Requests**: CPU 250m, Memory 512Mi
- **Limits**: CPU 500m, Memory 1024Mi
- **Strategy Service**: CPU 500m-1000m, Memory 768Mi-1536Mi (더 많은 리소스 필요)

필요에 따라 각 서비스의 YAML 파일에서 리소스 제한을 조정할 수 있습니다.

## ✅ 배포 체크리스트

### 사전 준비

- [ ] Docker가 설치되어 있고 실행 중인가?
- [ ] Docker Buildx가 설치되어 있는가? (`docker buildx version`)
- [ ] AWS CLI가 설치되고 인증 설정이 완료되었는가?
- [ ] kubectl이 설치되어 있고 EKS 클러스터에 연결되어 있는가?

### Kubernetes 클러스터 설정

- [ ] backend 네임스페이스가 생성되어 있는가?
- [ ] ECR Pull Secret이 생성되어 있는가? (`ecr-secret`)
- [ ] Redis가 배포되어 있는가? (redis-master.redis:6379)
- [ ] 충분한 클러스터 리소스가 있는가?

### 시크릿 설정

- [ ] `mariadb-secret`이 생성되어 있는가? (필수)
- [ ] `mongodb-secret`이 생성되어 있는가? (필수)
- [ ] `redis` secret이 생성되어 있는가? (필수)
- [ ] `kis-secret`이 생성되어 있는가? (선택사항)
- [ ] `openai-secret`이 생성되어 있는가? (선택사항)

### 배포 단계

- [ ] ECR에 이미지 빌드 및 푸시 완료
- [ ] Deployment YAML 적용 완료
- [ ] Pod가 Running 상태인가?
- [ ] 헬스체크가 통과하는가?

## 🔗 관련 문서

- [strategy-service 배포 가이드](../backend/strategy-service/DEPLOYMENT.md)
- [EKS 설정 가이드](../backend/strategy-service/EKS_SETUP_GUIDE.md)
