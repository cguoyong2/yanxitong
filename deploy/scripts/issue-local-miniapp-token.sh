#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${REPO_ROOT}/deploy/docker-compose.yml}"
DB_USERNAME="${DB_USERNAME:-yanxitong}"
DB_PASSWORD="${DB_PASSWORD:-yanxitong}"
DB_NAME="${DB_NAME:-yanxitong}"
OPEN_ID="${OPEN_ID:-local-acceptance-$(date +%Y%m%d%H%M%S)-$$}"
TOKEN_TTL_SECONDS="${TOKEN_TTL_SECONDS:-604800}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to issue a local miniapp token." >&2
  exit 1
fi

if ! docker compose -f "${COMPOSE_FILE}" ps --status running mysql redis >/dev/null 2>&1; then
  echo "Local MySQL and Redis containers must be running." >&2
  exit 1
fi

USER_ID="$(
  docker compose -f "${COMPOSE_FILE}" exec -T \
    -e MYSQL_PWD="${DB_PASSWORD}" mysql \
    mysql -N -B -u"${DB_USERNAME}" "${DB_NAME}" \
    -e "INSERT INTO miniapp_user (open_id, role_code, status, last_login_at)
        VALUES ('${OPEN_ID}', 'USER', 'ACTIVE', NOW())
        ON DUPLICATE KEY UPDATE status = 'ACTIVE', last_login_at = NOW();
        SELECT id FROM miniapp_user WHERE open_id = '${OPEN_ID}' LIMIT 1;"
)"

if [[ ! "${USER_ID}" =~ ^[0-9]+$ ]]; then
  echo "Unable to create the local miniapp user." >&2
  exit 1
fi

TOKEN="$(node -e "const crypto=require('crypto'); process.stdout.write(crypto.randomBytes(24).toString('hex'))")"
PRINCIPAL="$(
  USER_ID="${USER_ID}" OPEN_ID="${OPEN_ID}" node -e \
    "process.stdout.write(JSON.stringify({userId:Number(process.env.USER_ID),tenantId:null,openId:process.env.OPEN_ID,roleCode:'USER'}))"
)"

docker compose -f "${COMPOSE_FILE}" exec -T redis \
  redis-cli SETEX "auth:miniapp:${TOKEN}" "${TOKEN_TTL_SECONDS}" "${PRINCIPAL}" >/dev/null

printf '%s\n' "${TOKEN}"
