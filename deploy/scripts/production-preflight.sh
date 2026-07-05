#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${ENV_FILE:-${REPO_ROOT}/deploy/.env.production}"
BASE_URL="${BASE_URL:-http://127.0.0.1}"
SKIP_REMOTE_CHECKS="${SKIP_REMOTE_CHECKS:-0}"
REQUIRE_READINESS_READY="${REQUIRE_READINESS_READY:-1}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'EOF'
Usage: ENV_FILE=deploy/.env.production bash deploy/scripts/production-preflight.sh

Checks:
- required production env vars exist and are not placeholders
- mock payment success is disabled
- production frontend build artifacts exist
- optional remote /api/health and /api/health/readiness checks

Environment:
  ENV_FILE                 default deploy/.env.production
  BASE_URL                 default http://127.0.0.1
  SKIP_REMOTE_CHECKS=1     skip health/readiness HTTP checks
  REQUIRE_READINESS_READY=0 allow readiness/payment-provider status other than READY
EOF
  exit 0
fi

fail() {
  printf '[production-preflight] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[production-preflight] %s\n' "$*"
}

[[ -f "${ENV_FILE}" ]] || fail "env file not found: ${ENV_FILE}"

load_env_file() {
  local line name value
  while IFS= read -r line || [[ -n "${line}" ]]; do
    [[ -z "${line}" || "${line}" =~ ^[[:space:]]*# ]] && continue
    [[ "${line}" == *"="* ]] || continue
    name="${line%%=*}"
    value="${line#*=}"
    name="$(printf '%s' "${name}" | xargs)"
    value="${value%$'\r'}"
    if [[ "${value}" =~ ^\".*\"$ || "${value}" =~ ^\'.*\'$ ]]; then
      value="${value:1:${#value}-2}"
    fi
    [[ "${name}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    export "${name}=${value}"
  done < "${ENV_FILE}"
}

load_env_file

required_vars=(
  APP_ENV
  DB_URL
  DB_USERNAME
  DB_PASSWORD
  MYSQL_ROOT_PASSWORD
  REDIS_HOST
  REDIS_PORT
  REDIS_PASSWORD
  PAYMENT_DEFAULT_PROVIDER
  PAYMENT_MOCK_SUCCESS_ENABLED
  PAYMENT_MOCK_CALLBACK_SECRET
)

for name in "${required_vars[@]}"; do
  value="${!name:-}"
  [[ -n "${value}" ]] || fail "${name} is required"
  [[ "${value}" != *"replace-with"* ]] || fail "${name} still contains placeholder value"
done

[[ "${APP_ENV}" == "production" || "${APP_ENV}" == "prod" ]] || fail "APP_ENV must be production/prod"
[[ "${DB_PASSWORD}" != "yanxitong" ]] || fail "DB_PASSWORD must not use local default"
[[ "${REDIS_PASSWORD}" != "" ]] || fail "REDIS_PASSWORD must not be blank"
[[ "${PAYMENT_MOCK_SUCCESS_ENABLED}" == "false" ]] || fail "PAYMENT_MOCK_SUCCESS_ENABLED must be false"
[[ "${PAYMENT_MOCK_CALLBACK_SECRET}" != "yanxitong-mock-callback-secret" ]] || fail "PAYMENT_MOCK_CALLBACK_SECRET must not use local default"

is_placeholder() {
  local value="$1"
  [[ -z "${value}" || "${value}" == *"replace-with"* ]]
}

if [[ "${PAYMENT_DEFAULT_PROVIDER}" != "MOCK" ]]; then
  wechat_vars=(
    PAYMENT_WECHAT_SP_ENABLED
    PAYMENT_WECHAT_MERCHANT_ID
    PAYMENT_WECHAT_APP_ID
    PAYMENT_WECHAT_SERVICE_PROVIDER_ID
    PAYMENT_WECHAT_SUB_MERCHANT_ID
    PAYMENT_WECHAT_CERT_SERIAL_NO
    PAYMENT_WECHAT_PRIVATE_KEY_PATH
    PAYMENT_WECHAT_API_V3_KEY
    PAYMENT_WECHAT_NOTIFY_URL
  )
  if [[ "${REQUIRE_READINESS_READY}" == "1" ]]; then
    for name in "${wechat_vars[@]}"; do
      value="${!name:-}"
      [[ -n "${value}" ]] || fail "${name} is required when PAYMENT_DEFAULT_PROVIDER=${PAYMENT_DEFAULT_PROVIDER}"
      [[ "${value}" != *"replace-with"* ]] || fail "${name} still contains placeholder value"
    done
    [[ "${PAYMENT_WECHAT_SP_ENABLED}" == "true" ]] || fail "PAYMENT_WECHAT_SP_ENABLED must be true for real provider default"
    [[ "${PAYMENT_WECHAT_NOTIFY_URL}" == https://* ]] || fail "PAYMENT_WECHAT_NOTIFY_URL must be public HTTPS"
  else
    incomplete_wechat_vars=()
    for name in "${wechat_vars[@]}"; do
      value="${!name:-}"
      if is_placeholder "${value}"; then
        incomplete_wechat_vars+=("${name}")
      fi
    done
    if [[ "${#incomplete_wechat_vars[@]}" -gt 0 ]]; then
      log "Payment provider credentials deferred: ${incomplete_wechat_vars[*]}"
    fi
    if [[ -n "${PAYMENT_WECHAT_NOTIFY_URL:-}" && "${PAYMENT_WECHAT_NOTIFY_URL}" != https://* ]]; then
      fail "PAYMENT_WECHAT_NOTIFY_URL must be public HTTPS"
    fi
  fi
fi

[[ -f "${REPO_ROOT}/admin/dist/index.html" ]] || fail "admin/dist/index.html missing; run npm run build in admin"
[[ -f "${REPO_ROOT}/confirm-screen/dist/index.html" ]] || fail "confirm-screen/dist/index.html missing; run npm run build in confirm-screen"

if [[ "${SKIP_REMOTE_CHECKS}" == "1" ]]; then
  log "Remote checks skipped."
  log "Production preflight passed."
  exit 0
fi

curl -fsS "${BASE_URL}/api/health" >/dev/null || fail "health check failed: ${BASE_URL}/api/health"
readiness_file="$(mktemp)"
curl -fsS "${BASE_URL}/api/health/readiness" >"${readiness_file}" || fail "readiness check failed: ${BASE_URL}/api/health/readiness"
readiness_status="$(node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); console.log(data.data?.status || 'UNKNOWN')" "${readiness_file}")"
rm -f "${readiness_file}"
log "Readiness status: ${readiness_status}"
if [[ "${REQUIRE_READINESS_READY}" == "1" && "${readiness_status}" != "READY" ]]; then
  fail "readiness must be READY"
fi

log "Production preflight passed."
