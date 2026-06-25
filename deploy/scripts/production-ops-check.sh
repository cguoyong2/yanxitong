#!/usr/bin/env bash
set -euo pipefail

SSH_TARGET="${SSH_TARGET:-root@115.29.229.188}"
BASE_URL="${BASE_URL:-https://yxt.yqej.cn}"
BASE_URL="${BASE_URL%/}"
BACKUP_DIR="${BACKUP_DIR:-/opt/backups/yanxitong/mysql}"
MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-24}"
DISK_WARN_PERCENT="${DISK_WARN_PERCENT:-80}"
DISK_FAIL_PERCENT="${DISK_FAIL_PERCENT:-90}"
LOG_SINCE="${LOG_SINCE:-1h}"
LOG_ERROR_FAIL="${LOG_ERROR_FAIL:-0}"
REQUIRE_READINESS_READY="${REQUIRE_READINESS_READY:-0}"

failures=()
warnings=()

add_failure() {
  failures+=("$1")
  echo "[FAIL] $1" >&2
}

add_warning() {
  warnings+=("$1")
  echo "[WARN] $1" >&2
}

pass() {
  echo "[PASS] $1"
}

http_status() {
  local url="$1"
  curl -sS -o /tmp/yanxitong-ops-check-response.$$ -w "%{http_code}" "$url"
}

check_http() {
  local name="$1"
  local url="$2"
  local expected="${3:-200}"
  local status
  if ! status="$(http_status "$url")"; then
    add_failure "${name} request failed: ${url}"
    return
  fi
  if [[ "$status" == "$expected" ]]; then
    pass "${name}: HTTP ${status}"
  else
    add_failure "${name}: expected HTTP ${expected}, got ${status}"
  fi
}

check_http "backend health" "${BASE_URL}/api/health" "200"
check_http "admin login page" "${BASE_URL}/login" "200"
check_http "confirm-screen bind page" "${BASE_URL}/confirm-screen/bind" "200"
check_http "edge healthz" "${BASE_URL}/healthz" "200"

readiness_payload="$(curl -fsS "${BASE_URL}/api/health/readiness" || true)"
if [[ -z "$readiness_payload" ]]; then
  add_failure "readiness endpoint is not reachable"
else
  readiness_status="$(node -e "const data=JSON.parse(process.argv[1]); console.log(data.data?.status || '')" "$readiness_payload")"
  if [[ "$readiness_status" == "READY" ]]; then
    pass "readiness: READY"
  elif [[ "$REQUIRE_READINESS_READY" == "1" ]]; then
    add_failure "readiness is ${readiness_status}, expected READY"
  else
    add_warning "readiness is ${readiness_status}; acceptable before real payment launch"
  fi
fi

remote_output="$(ssh "${SSH_TARGET}" \
  "BACKUP_DIR='${BACKUP_DIR}' MAX_BACKUP_AGE_HOURS='${MAX_BACKUP_AGE_HOURS}' DISK_WARN_PERCENT='${DISK_WARN_PERCENT}' DISK_FAIL_PERCENT='${DISK_FAIL_PERCENT}' LOG_SINCE='${LOG_SINCE}' LOG_ERROR_FAIL='${LOG_ERROR_FAIL}' bash -s" <<'REMOTE'
set -euo pipefail

failures=()
warnings=()

add_failure() {
  failures+=("$1")
  echo "[FAIL] $1"
}

add_warning() {
  warnings+=("$1")
  echo "[WARN] $1"
}

pass() {
  echo "[PASS] $1"
}

expected_containers=(
  global-edge-nginx
  yanxitong-web
  yanxitong-server
  yanxitong-mysql
  yanxitong-redis
)

for container in "${expected_containers[@]}"; do
  if docker ps --format '{{.Names}}' | grep -qx "$container"; then
    pass "container running: ${container}"
  else
    add_failure "container not running: ${container}"
  fi
done

for path in / /opt /var/lib/docker; do
  if [[ -d "$path" ]]; then
    used="$(df -P "$path" | awk 'NR==2 { gsub("%", "", $5); print $5 }')"
    if [[ "$used" -ge "$DISK_FAIL_PERCENT" ]]; then
      add_failure "disk usage ${path}: ${used}% >= ${DISK_FAIL_PERCENT}%"
    elif [[ "$used" -ge "$DISK_WARN_PERCENT" ]]; then
      add_warning "disk usage ${path}: ${used}% >= ${DISK_WARN_PERCENT}%"
    else
      pass "disk usage ${path}: ${used}%"
    fi
  fi
done

if docker exec yanxitong-web nginx -t >/tmp/yanxitong-web-nginx-check.log 2>&1; then
  pass "yanxitong-web nginx config"
else
  add_failure "yanxitong-web nginx config failed: $(tr '\n' ' ' < /tmp/yanxitong-web-nginx-check.log)"
fi

if docker exec global-edge-nginx nginx -t >/tmp/yanxitong-edge-nginx-check.log 2>&1; then
  pass "global-edge-nginx config"
else
  add_failure "global-edge-nginx config failed: $(tr '\n' ' ' < /tmp/yanxitong-edge-nginx-check.log)"
fi

if docker exec yanxitong-mysql sh -c 'mysqladmin ping -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' >/dev/null; then
  pass "mysql ping"
else
  add_failure "mysql ping failed"
fi

if docker exec yanxitong-redis sh -c 'redis-cli -a "$REDIS_PASSWORD" ping 2>/dev/null | grep -qx PONG' >/dev/null; then
  pass "redis ping"
else
  add_failure "redis ping failed"
fi

if [[ -d "$BACKUP_DIR" ]]; then
  latest_backup="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name '*.sql.gz' -printf '%T@ %p\n' | sort -nr | head -1 | cut -d' ' -f2-)"
  if [[ -z "$latest_backup" ]]; then
    add_failure "no database backup found in ${BACKUP_DIR}"
  else
    age_seconds="$(( $(date +%s) - $(stat -c %Y "$latest_backup") ))"
    max_age_seconds="$(( MAX_BACKUP_AGE_HOURS * 3600 ))"
    if [[ "$age_seconds" -gt "$max_age_seconds" ]]; then
      add_failure "latest backup is older than ${MAX_BACKUP_AGE_HOURS}h: ${latest_backup}"
    else
      pass "latest backup age within ${MAX_BACKUP_AGE_HOURS}h: ${latest_backup}"
    fi
    if [[ -f "${latest_backup}.sha256" ]] && (cd "$(dirname "$latest_backup")" && sha256sum -c "$(basename "${latest_backup}.sha256")" >/dev/null); then
      pass "latest backup checksum"
    else
      add_failure "latest backup checksum missing or failed: ${latest_backup}.sha256"
    fi
  fi
else
  add_failure "backup directory missing: ${BACKUP_DIR}"
fi

server_errors="$(docker logs --since "$LOG_SINCE" yanxitong-server 2>&1 | { grep -E '(^|[[:space:]])(ERROR|Exception|Caused by:)' || true; } | wc -l | tr -d ' ')"
edge_5xx="$(docker logs --since "$LOG_SINCE" global-edge-nginx 2>&1 | awk '$9 ~ /^5[0-9][0-9]$/ { count++ } END { print count + 0 }')"
web_5xx="$(docker logs --since "$LOG_SINCE" yanxitong-web 2>&1 | awk '$9 ~ /^5[0-9][0-9]$/ { count++ } END { print count + 0 }')"

if [[ "$server_errors" -gt 0 ]]; then
  if [[ "$LOG_ERROR_FAIL" == "1" ]]; then
    add_failure "yanxitong-server recent ERROR/Exception count (${LOG_SINCE}): ${server_errors}"
  else
    add_warning "yanxitong-server recent ERROR/Exception count (${LOG_SINCE}): ${server_errors}"
  fi
else
  pass "yanxitong-server recent ERROR/Exception count (${LOG_SINCE}): 0"
fi

if [[ "$edge_5xx" -gt 0 ]]; then
  add_warning "global-edge-nginx recent 5xx count (${LOG_SINCE}): ${edge_5xx}"
else
  pass "global-edge-nginx recent 5xx count (${LOG_SINCE}): 0"
fi

if [[ "$web_5xx" -gt 0 ]]; then
  add_warning "yanxitong-web recent 5xx count (${LOG_SINCE}): ${web_5xx}"
else
  pass "yanxitong-web recent 5xx count (${LOG_SINCE}): 0"
fi

printf '__REMOTE_SUMMARY__ failures=%s warnings=%s\n' "${#failures[@]}" "${#warnings[@]}"
if [[ "${#failures[@]}" -gt 0 ]]; then
  exit 2
fi
REMOTE
)" || remote_status=$?
remote_status="${remote_status:-0}"

echo "$remote_output"

if [[ "$remote_status" -ne 0 ]]; then
  add_failure "remote ops checks failed"
fi

rm -f /tmp/yanxitong-ops-check-response.$$

echo
echo "Production ops check summary:"
echo "- failures: ${#failures[@]}"
echo "- warnings: ${#warnings[@]}"
echo "- base URL: ${BASE_URL}"
echo "- ssh target: ${SSH_TARGET}"

if [[ "${#failures[@]}" -gt 0 ]]; then
  exit 1
fi
