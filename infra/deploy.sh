#!/bin/bash

echo "========== 무중단 배포 프로세스 시작 =========="

# 명령어 실패 시 스크립트 중단
set -euo pipefail

# 작업 디렉토리 이동 및 환경 변수 로드
BASE_DIR="/home/ubuntu/synq"
cd "$BASE_DIR" || exit 1

if [ ! -f "$BASE_DIR/.env" ]; then
    echo "[ERROR] .env 파일이 존재하지 않습니다. 배포를 중단합니다."
    exit 1
fi

set -a
source "$BASE_DIR/.env"
set +a

# 필수 환경 변수 검증
required_vars=(
    DOCKERHUB_USERNAME PROD_DB_NAME PROD_DB_USER PROD_DB_PASSWORD
    PROD_REDIS_PASSWORD PROD_JWT_SECRET PROD_GEMINI_API_KEY PROD_OPENAI_API_KEY
    PROD_KAKAO_CLIENT_ID PROD_KAKAO_CLIENT_SECRET PROD_KAKAO_REDIRECT_URI
    PROD_NAVER_CLIENT_ID PROD_NAVER_CLIENT_SECRET PROD_CORS_ALLOWED_ORIGINS
)
for var_name in "${required_vars[@]}"; do
    if [ -z "${!var_name:-}" ]; then
        echo "[ERROR] 필수 환경 변수 누락: $var_name"
        exit 1
    fi
done

COMPOSE_FILE="infra/docker-compose.prod.yml"
NGINX_CONF="infra/nginx/conf.d/default.conf"

# 인프라 서비스(DB, Redis) 가동 확인
echo "=========================================="
echo "[1/5] DB, Redis 가동"
echo "=========================================="
docker compose -f $COMPOSE_FILE up -d db redis

# 활성 컨테이너 식별 (Nginx 설정 파일 기준)
echo "=========================================="
echo "[2/5] 현재 Active 상태 식별"
echo "=========================================="
if grep -q "server springboot-blue:8080" "$NGINX_CONF"; then
    TARGET_COLOR="green"
    TARGET_PORT=8082
    CURRENT_COLOR="blue"
elif grep -q "server springboot-green:8080" "$NGINX_CONF"; then
    TARGET_COLOR="blue"
    TARGET_PORT=8081
    CURRENT_COLOR="green"
else
    echo "[ERROR] 활성 컨테이너를 식별할 수 없습니다. 배포를 중단합니다."
    exit 1
fi

echo "현재 컨테이너: $CURRENT_COLOR"
echo "신규 컨테이너: $TARGET_COLOR"

# 신규 컨테이너 가동
echo "=========================================="
echo "[3/5] $TARGET_COLOR 컨테이너 가동"
echo "=========================================="
docker compose -f $COMPOSE_FILE pull springboot-$TARGET_COLOR
docker compose -f $COMPOSE_FILE up -d springboot-$TARGET_COLOR

# 신규 컨테이너 헬스 체크
echo "=========================================="
echo "[4/5] $TARGET_COLOR 컨테이너 헬스 체크 시작"
echo "=========================================="

MAX_RETRIES=15
RETRY_COUNT=0
HEALTH_CHECK_PASSED=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    RETRY_COUNT=$((RETRY_COUNT+1))

    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        --connect-timeout 2 --max-time 5 \
        "http://127.0.0.1:$TARGET_PORT/actuator/health" || true)

    if [ "$HTTP_STATUS" -eq 200 ]; then
        echo "$TARGET_COLOR 컨테이너 헬스 체크 통과."
        HEALTH_CHECK_PASSED=true
        break
    else
        sleep 3
    fi
done

# 헬스 체크 실패 시 배포 중단 및 롤백
if [ "$HEALTH_CHECK_PASSED" = false ]; then
    echo "[ERROR] $TARGET_COLOR 컨테이너 헬스 체크 실패. 배포를 중단합니다."
    docker compose -f $COMPOSE_FILE stop springboot-$TARGET_COLOR
    exit 1
fi

# Nginx 트래픽 포트 스위칭 & Reload
echo "=========================================="
echo "[5/5] Nginx 라우팅 스위칭: $CURRENT_COLOR ➔ $TARGET_COLOR"
echo "=========================================="

# 백업 생성 및 Nginx 스위칭
cp "$NGINX_CONF" "${NGINX_CONF}.bak"
sed -i "s/server springboot-${CURRENT_COLOR}:8080/server springboot-${TARGET_COLOR}:8080/g" "$NGINX_CONF"

# Nginx 최초 기동 보장
docker compose -f $COMPOSE_FILE up -d nginx

if docker exec synq-nginx nginx -t && docker exec synq-nginx nginx -s reload; then

    # Nginx Smoke Test 추가
    echo "Nginx Smoke Test 진행 중..."
    SMOKE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost/actuator/health" || true)

    if [ "$SMOKE_STATUS" -eq 200 ]; then
        echo "라우팅 스위칭 및 Smoke Test 완료."
        rm "${NGINX_CONF}.bak"
    else
        echo "[ERROR] Smoke Test 실패 (HTTP $SMOKE_STATUS). Nginx 라우팅 오류가 의심되어 Rollback 합니다."
        mv "${NGINX_CONF}.bak" "$NGINX_CONF"
        docker exec synq-nginx nginx -s reload
        docker compose -f $COMPOSE_FILE stop springboot-$TARGET_COLOR
        exit 1
    fi
else
    echo "[ERROR] 라우팅 스위칭 실패. 원본으로 Rollback 합니다."
    mv "${NGINX_CONF}.bak" "$NGINX_CONF"
    docker compose -f $COMPOSE_FILE stop springboot-$TARGET_COLOR
    exit 1
fi

echo "신규 $TARGET_COLOR 컨테이너가 서비스를 수신합니다."

# 7. Graceful Drain 대기 후 구버전 컨테이너 종료
echo "잔여 요청 처리를 위해 5초간 대기..."
sleep 5

echo "구버전 ($CURRENT_COLOR) 컨테이너 종료 (Graceful Shutdown 유예 적용)..."
docker compose -f $COMPOSE_FILE stop -t 90 springboot-$CURRENT_COLOR

echo "댕글링 이미지 정리..."
docker image prune -f

echo "========== 무중단 배포 프로세스 완료 =========="