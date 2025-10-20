#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  AMD64 이미지 빌드 및 푸시${NC}"
echo -e "${GREEN}======================================${NC}\n"

# ECR 설정
AWS_REGION="ap-northeast-2"
AWS_ACCOUNT_ID="618221165332"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

# 빌드할 서비스 목록
SERVICES=("auth-service" "trading-service" "strategy-service")

echo -e "${YELLOW}🔐 ECR 로그인 중...${NC}"
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ ECR 로그인 실패${NC}"
    exit 1
fi
echo -e "${GREEN}✅ ECR 로그인 성공${NC}\n"

# 각 서비스별 AMD64 빌드 및 푸시
SUCCESS_COUNT=0
FAIL_COUNT=0
FAILED_SERVICES=()

for SERVICE in "${SERVICES[@]}"; do
    echo -e "${YELLOW}🐳 ${SERVICE} AMD64 이미지 빌드 및 푸시 중...${NC}"
    
    ECR_TAG="${ECR_REGISTRY}/${SERVICE}:latest"
    
    # AMD64 전용 빌드 및 푸시
    docker buildx build \
        --platform linux/amd64 \
        --tag "${ECR_TAG}" \
        --push \
        -f "${SERVICE}/Dockerfile" \
        .
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} AMD64 빌드 및 푸시 성공${NC}\n"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo -e "${RED}❌ ${SERVICE} AMD64 빌드 및 푸시 실패${NC}\n"
        FAIL_COUNT=$((FAIL_COUNT + 1))
        FAILED_SERVICES+=("$SERVICE")
    fi
done

# 빌드 결과 요약
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}           빌드 결과 요약${NC}"
echo -e "${GREEN}======================================${NC}"
echo -e "성공: ${GREEN}${SUCCESS_COUNT}${NC} / 실패: ${RED}${FAIL_COUNT}${NC} / 전체: $((SUCCESS_COUNT + FAIL_COUNT))"

if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
    echo -e "\n${RED}실패한 서비스:${NC}"
    for SERVICE in "${FAILED_SERVICES[@]}"; do
        echo -e "  - ${RED}${SERVICE}${NC}"
    done
fi

echo -e "\n${YELLOW}📋 ECR에 푸시된 AMD64 이미지:${NC}"
for SERVICE in "${SERVICES[@]}"; do
    echo -e "  - ${BLUE}${ECR_REGISTRY}/${SERVICE}:latest${NC} (linux/amd64)"
done

echo -e "\n${GREEN}✅ AMD64 빌드 및 푸시 완료!${NC}"

# 실패한 서비스가 있으면 종료 코드 1 반환
if [ $FAIL_COUNT -gt 0 ]; then
    exit 1
fi
