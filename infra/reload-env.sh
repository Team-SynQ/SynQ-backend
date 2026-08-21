#!/bin/bash
# 현재 활성 컨테이너를 "새 이미지 pull 없이" .env 변경 사항만 반영해 재기동한다.
# CI/CD(deploy.sh)와 달리 GHCR 인증이 필요 없다 — 로컬에 이미 있는 이미지를 그대로 재사용한다.
# 코드 버전을 바꾸고 싶을 때는 이 스크립트가 아니라 deploy.sh(CI/CD)를 사용해야 한다.
#
# 실행: bash infra/reload-env.sh
# CI가 매 배포마다 infra/ 디렉토리를 통째로 서버에 재복사하면서 실행 권한이 초기화되므로,
# ./infra/reload-env.sh 로는 chmod +x 를 매번 다시 해줘야 할 수 있다. bash로 실행하면
# 실행 권한과 무관하게 항상 동작한다.

echo "========== 환경 변수 리로드 시작 =========="

set -euo pipefail

BASE_DIR="/home/ubuntu/synq"
cd "$BASE_DIR" || exit 1

if [ ! -f "$BASE_DIR/.env" ]; then
    echo "[ERROR] .env 파일이 존재하지 않습니다. 리로드를 중단합니다."
    exit 1
fi

COMPOSE_FILE="infra/docker-compose.prod.yml"

# 현재 활성 컨테이너 식별
IS_BLUE=$(docker ps -q -f "name=^/?synq-backend-blue$")
IS_GREEN=$(docker ps -q -f "name=^/?synq-backend-green$")

if [ -n "$IS_BLUE" ]; then
    ACTIVE_COLOR="blue"
    ACTIVE_PORT=8081
elif [ -n "$IS_GREEN" ]; then
    ACTIVE_COLOR="green"
    ACTIVE_PORT=8082
else
    echo "[ERROR] 활성화된 컨테이너가 없습니다. 최초 배포는 deploy.sh(CI/CD)를 사용하세요."
    exit 1
fi

echo "대상 컨테이너: $ACTIVE_COLOR (이미지는 재다운로드하지 않음)"

# 이미지는 그대로 두고, 변경된 .env 값만 반영해 컨테이너 재생성
docker compose -f "$COMPOSE_FILE" up -d springboot-$ACTIVE_COLOR

# 재기동 후 헬스 체크
echo "헬스 체크 진행 중..."
MAX_RETRIES=15
RETRY_COUNT=0
HEALTH_CHECK_PASSED=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    RETRY_COUNT=$((RETRY_COUNT+1))

    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        --connect-timeout 2 --max-time 5 \
        "http://127.0.0.1:$ACTIVE_PORT/actuator/health" || true)

    if [ "$HTTP_STATUS" -eq 200 ]; then
        HEALTH_CHECK_PASSED=true
        break
    else
        sleep 3
    fi
done

if [ "$HEALTH_CHECK_PASSED" = true ]; then
    echo "리로드 완료. $ACTIVE_COLOR 컨테이너 정상 응답."
else
    echo "[ERROR] 리로드 후 헬스 체크 실패. 컨테이너 로그를 확인하세요: docker logs synq-backend-${ACTIVE_COLOR}"
    exit 1
fi

echo "========== 환경 변수 리로드 완료 =========="
