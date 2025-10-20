#!/bin/bash
# Trading Service Docker 빌드 및 ECR 푸시 스크립트

set -e  # 에러 발생 시 중단

echo "🔐 ECR 로그인 중..."
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin 618221165332.dkr.ecr.ap-northeast-2.amazonaws.com

echo ""
echo "🐳 Docker 이미지 빌드 및 푸시 시작..."
echo "   플랫폼: linux/amd64"
echo "   이미지: 618221165332.dkr.ecr.ap-northeast-2.amazonaws.com/trading-service:latest"
echo ""

docker buildx build \
  --platform linux/amd64 \
  --file trading-service/Dockerfile \
  --tag 618221165332.dkr.ecr.ap-northeast-2.amazonaws.com/trading-service:latest \
  --tag 618221165332.dkr.ecr.ap-northeast-2.amazonaws.com/trading-service:$(date +%Y%m%d-%H%M%S) \
  --push \
  --progress=plain \
  .

echo ""
echo "✅ 빌드 및 푸시 완료!"
echo ""
echo "🔄 Kubernetes 배포 업데이트 (선택사항):"
echo "   kubectl rollout restart deployment trading-service -n backend"
echo "   kubectl rollout status deployment trading-service -n backend"



