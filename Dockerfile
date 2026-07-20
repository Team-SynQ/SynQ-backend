# ===== 1단계: 빌드 스테이지 =====
FROM gradle:8.12-jdk17 AS build
WORKDIR /home/gradle/project

# 빌드 속도 향상을 위한 의존성 캐싱 레이어 구성
COPY settings.gradle build.gradle gradle.properties* ./
COPY gradle ./gradle
RUN gradle --no-daemon build -x test || true

# 소스 코드 복사
COPY . .
# 테스트 가동 환경 분리 빌드
RUN gradle --no-daemon clean bootJar -x test

# jar 파일 추출하여 app.jar로 명명
RUN JAR_FILE="$(ls build/libs | grep -E '\.jar$' | grep -v 'plain' | head -n 1)" \
 && cp "build/libs/${JAR_FILE}" /home/gradle/project/app.jar


# ===== 2단계: 런타임 스테이지 =====
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app

# 타임존 설정
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone

# app.jar 복사
COPY --from=build /home/gradle/project/app.jar app.jar

# 포트 명시
EXPOSE 8080

# 컨테이너 실행 커맨드
ENTRYPOINT ["java", "-jar", "-Duser.timezone=Asia/Seoul", "app.jar"]