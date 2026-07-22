#!/bin/bash

# 0. 명령어 실패 시 스크립트 중단
set -euo pipefail

# 1. 작업 디렉토리 이동 및 환경 변수 로드
BASE_DIR="/home/ubuntu/synq"
cd "$BASE_DIR" || exit 1

if [ ! -f "$BASE_DIR/.env" ]; then
    echo "[ERROR] .env 파일이 존재하지 않습니다: $BASE_DIR/.env"
    exit 1
fi

set -a
source "$BASE_DIR/.env"
set +a

COMPOSE_FILE="infra/docker-compose.prod.yml"
NGINX_CONF="infra/nginx/conf.d/default.conf"

echo "=========================================="
echo "[1/5] 무중단 배포 프로세스 시작"
echo "=========================================="

# 2. 인프라 서비스(DB, Redis, Nginx) 가동 확인
echo "=========================================="
echo "[2/5] 인프라 서비스(PostgreSQL, Redis, Nginx) 가동"
echo "=========================================="
docker compose -f $COMPOSE_FILE up -d db redis nginx

# 3. 현재 가동 중인 스프링 컨테이너 확인 (Nginx 설정 파일 기준)
ACTIVE_PORT=$(grep -oE ':(8081|8082)' "$NGINX_CONF" | head -n 1 | tr -d ':' || true)

if [ "$ACTIVE_PORT" = "8082" ]; then
    # 현재 활성 포트가 8082이면 -> Target: Blue (8081) / Current: Green (8082)
    TARGET_COLOR="blue"
    TARGET_PORT=8081
    CURRENT_COLOR="green"
    CURRENT_PORT=8082
else
    # 현재 활성 포트가 8081이면 -> Target: Green (8082) / Current: Blue (8081)
    TARGET_COLOR="green"
    TARGET_PORT=8082
    CURRENT_COLOR="blue"
    CURRENT_PORT=8081
fi

echo "현재 컨테이너: $CURRENT_COLOR ($CURRENT_PORT)"
echo "신규 컨테이너: $TARGET_COLOR ($TARGET_PORT)"

# 4. 신규 컨테이너 가동
echo "=========================================="
echo "[3/5] $TARGET_COLOR 컨테이너 가동"
echo "=========================================="
docker compose -f $COMPOSE_FILE pull springboot-$TARGET_COLOR
docker compose -f $COMPOSE_FILE up -d springboot-$TARGET_COLOR

# 5. 신규 컨테이너 헬스 체크
echo "=========================================="
echo "[4/5] $TARGET_COLOR 컨테이너 헬스 체크 시작 (http://127.0.0.1:$TARGET_PORT/actuator/health)..."
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
        echo "[$RETRY_COUNT/$MAX_RETRIES] $TARGET_COLOR 컨테이너 부팅 완료. (HTTP 200 OK)"
        HEALTH_CHECK_PASSED=true
        break
    else
        echo "[$RETRY_COUNT/$MAX_RETRIES] $TARGET_COLOR 컨테이너 부팅 대기 중... (응답 코드: $HTTP_STATUS)"
        sleep 3
    fi
done

# 헬스 체크 실패 시 배포 중단 및 롤백
if [ "$HEALTH_CHECK_PASSED" = false ]; then
    echo "=========================================="
    echo "[ERROR] $TARGET_COLOR 컨테이너 헬스 체크 실패."
    echo "신규 컨테이너를 중지하고 배포를 중단."
    echo "=========================================="
    docker compose -f $COMPOSE_FILE stop springboot-$TARGET_COLOR
    exit 1
fi

# 6. Nginx 트래픽 포트 스위칭 & Reload
echo "=========================================="
echo "[5/5] Nginx 포트 스위칭: $CURRENT_PORT ➔ $TARGET_PORT"
echo "=========================================="

# default.conf 포트 번호를 Target 포트로 변경
sed -i "s/$CURRENT_PORT/$TARGET_PORT/g" $NGINX_CONF

# Nginx 검증 후 Reload
docker exec synq-nginx nginx -t
docker exec synq-nginx nginx -s reload

echo "포트 스위칭 완료. 신규 $TARGET_COLOR 컨테이너가 서비스를 수신."

# 7. Graceful Drain 대기 후 구버전 컨테이너 종료
echo "잔여 요청 처리를 위해 5초간 대기..."
sleep 5

echo "구버전 ($CURRENT_COLOR) 컨테이너 종료."
docker compose -f $COMPOSE_FILE stop springboot-$CURRENT_COLOR

echo "=========================================="
echo "[COMPLETE] 무중단 배포 완료."
echo "=========================================="