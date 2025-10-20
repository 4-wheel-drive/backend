# ======================
# Stage 1: Build
# ======================
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Gradle 래퍼 및 설정 파일 복사
COPY settings.gradle .
COPY build.gradle .
COPY gradlew .
COPY gradle ./gradle

# 공통 서비스 소스 복사 (의존성)
COPY common-service ./common-service

# strategy-service 소스 복사
COPY strategy-service ./strategy-service

# 의존성 다운로드 및 빌드 (캐시 활용)
RUN ./gradlew :strategy-service:build -x test --no-daemon

# ======================
# Stage 2: Runtime
# ======================
FROM eclipse-temurin:17-jre

WORKDIR /app

# 애플리케이션 사용자 생성 (보안)
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# 빌드된 JAR 파일 복사
COPY --from=build /app/strategy-service/build/libs/*.jar app.jar

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# 포트 노출
EXPOSE 8081

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

