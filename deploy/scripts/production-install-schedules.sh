#!/usr/bin/env bash
set -euo pipefail

SSH_TARGET="${SSH_TARGET:-root@115.29.229.188}"
OPS_DIR="${OPS_DIR:-/opt/apps/yanxitong/ops}"
BACKUP_DIR="${BACKUP_DIR:-/opt/backups/yanxitong/mysql}"
LOG_DIR="${LOG_DIR:-/opt/backups/yanxitong/ops-logs}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-yanxitong-mysql}"
DATABASE="${DATABASE:-yanxitong}"
BASE_URL="${BASE_URL:-https://yxt.yqej.cn}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
LOG_RETENTION_DAYS="${LOG_RETENTION_DAYS:-14}"
MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-26}"
DISK_WARN_PERCENT="${DISK_WARN_PERCENT:-80}"
DISK_FAIL_PERCENT="${DISK_FAIL_PERCENT:-90}"
LOG_SINCE="${LOG_SINCE:-1h}"
BACKUP_CRON="${BACKUP_CRON:-10 3 * * *}"
OPS_CHECK_CRON="${OPS_CHECK_CRON:-*/30 * * * *}"
CRON_MARKER_BEGIN="# BEGIN YANXITONG OPS"
CRON_MARKER_END="# END YANXITONG OPS"

ssh "${SSH_TARGET}" \
  "OPS_DIR='${OPS_DIR}' BACKUP_DIR='${BACKUP_DIR}' LOG_DIR='${LOG_DIR}' MYSQL_CONTAINER='${MYSQL_CONTAINER}' DATABASE='${DATABASE}' BASE_URL='${BASE_URL}' BACKUP_RETENTION_DAYS='${BACKUP_RETENTION_DAYS}' LOG_RETENTION_DAYS='${LOG_RETENTION_DAYS}' MAX_BACKUP_AGE_HOURS='${MAX_BACKUP_AGE_HOURS}' DISK_WARN_PERCENT='${DISK_WARN_PERCENT}' DISK_FAIL_PERCENT='${DISK_FAIL_PERCENT}' LOG_SINCE='${LOG_SINCE}' BACKUP_CRON='${BACKUP_CRON}' OPS_CHECK_CRON='${OPS_CHECK_CRON}' CRON_MARKER_BEGIN='${CRON_MARKER_BEGIN}' CRON_MARKER_END='${CRON_MARKER_END}' bash -s" <<'REMOTE'
set -euo pipefail

mkdir -p "${OPS_DIR}" "${BACKUP_DIR}" "${LOG_DIR}"

cat > "${OPS_DIR}/backup-mysql.sh" <<'BACKUP_SCRIPT'
#!/usr/bin/env bash
set -euo pipefail

MYSQL_CONTAINER="${MYSQL_CONTAINER:-yanxitong-mysql}"
DATABASE="${DATABASE:-yanxitong}"
BACKUP_DIR="${BACKUP_DIR:-/opt/backups/yanxitong/mysql}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
RUN_ID="$(date +%Y%m%d%H%M%S)"
BACKUP_PATH="${BACKUP_DIR%/}/${DATABASE}-${RUN_ID}.sql.gz"
SHA_PATH="${BACKUP_PATH}.sha256"
MANIFEST_PATH="${BACKUP_PATH}.manifest.json"

mkdir -p "${BACKUP_DIR}"

if ! docker ps --format '{{.Names}}' | grep -qx "${MYSQL_CONTAINER}"; then
  echo "MySQL container is not running: ${MYSQL_CONTAINER}" >&2
  exit 1
fi

tmp_path="${BACKUP_PATH}.tmp"
rm -f "${tmp_path}" "${SHA_PATH}" "${MANIFEST_PATH}"

docker exec \
  -e BACKUP_DATABASE="${DATABASE}" \
  "${MYSQL_CONTAINER}" \
  sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --quick --routines --triggers --events --hex-blob --set-gtid-purged=OFF "$BACKUP_DATABASE"' \
  | gzip -9 > "${tmp_path}"

mv "${tmp_path}" "${BACKUP_PATH}"
sha256sum "${BACKUP_PATH}" > "${SHA_PATH}"

size_bytes="$(wc -c < "${BACKUP_PATH}" | tr -d ' ')"
sha256_value="$(cut -d' ' -f1 "${SHA_PATH}")"
created_at="$(date -Iseconds)"
cat > "${MANIFEST_PATH}" <<JSON
{
  "createdAt": "${created_at}",
  "database": "${DATABASE}",
  "mysqlContainer": "${MYSQL_CONTAINER}",
  "backupPath": "${BACKUP_PATH}",
  "sha256Path": "${SHA_PATH}",
  "sha256": "${sha256_value}",
  "sizeBytes": ${size_bytes}
}
JSON

if [[ -n "${RETENTION_DAYS}" ]]; then
  find "${BACKUP_DIR}" -type f -name '*.sql.gz' -mtime "+${RETENTION_DAYS}" -print -delete
  find "${BACKUP_DIR}" -type f \( -name '*.sql.gz.sha256' -o -name '*.sql.gz.manifest.json' \) -mtime "+${RETENTION_DAYS}" -print -delete
fi

echo "Backup path: ${BACKUP_PATH}"
echo "Checksum: ${SHA_PATH}"
echo "Manifest: ${MANIFEST_PATH}"
BACKUP_SCRIPT

cat > "${OPS_DIR}/ops-check.sh" <<'OPS_SCRIPT'
#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://yxt.yqej.cn}"
BASE_URL="${BASE_URL%/}"
BACKUP_DIR="${BACKUP_DIR:-/opt/backups/yanxitong/mysql}"
MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-26}"
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
  echo "[WARN] $1"
}

pass() {
  echo "[PASS] $1"
}

check_http() {
  local name="$1"
  local url="$2"
  local expected="${3:-200}"
  local status
  if ! status="$(curl -sS -o /tmp/yanxitong-ops-check-response.$$ -w "%{http_code}" "$url")"; then
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
  readiness_status="$(printf '%s' "$readiness_payload" | sed -n 's/.*"status"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)"
  if [[ "$readiness_status" == "READY" ]]; then
    pass "readiness: READY"
  elif [[ "$REQUIRE_READINESS_READY" == "1" ]]; then
    add_failure "readiness is ${readiness_status}, expected READY"
  else
    add_warning "readiness is ${readiness_status}; acceptable before real payment launch"
  fi
fi

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

rm -f /tmp/yanxitong-ops-check-response.$$

echo "summary failures=${#failures[@]} warnings=${#warnings[@]}"
if [[ "${#failures[@]}" -gt 0 ]]; then
  exit 1
fi
OPS_SCRIPT

cat > "${OPS_DIR}/run-backup-cron.sh" <<'BACKUP_RUNNER'
#!/usr/bin/env bash
set -euo pipefail
LOG_DIR="${LOG_DIR:-/opt/backups/yanxitong/ops-logs}"
LOG_RETENTION_DAYS="${LOG_RETENTION_DAYS:-14}"
mkdir -p "$LOG_DIR"
log_file="${LOG_DIR}/backup-mysql-$(date +%Y%m%d).log"
{
  echo "===== $(date -Iseconds) backup start ====="
  MYSQL_CONTAINER="${MYSQL_CONTAINER:-yanxitong-mysql}" DATABASE="${DATABASE:-yanxitong}" BACKUP_DIR="${BACKUP_DIR:-/opt/backups/yanxitong/mysql}" RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}" /opt/apps/yanxitong/ops/backup-mysql.sh
  echo "===== $(date -Iseconds) backup ok ====="
} >> "$log_file" 2>&1
find "$LOG_DIR" -type f -name '*.log' -mtime "+${LOG_RETENTION_DAYS}" -delete
BACKUP_RUNNER

cat > "${OPS_DIR}/run-ops-check-cron.sh" <<'OPS_RUNNER'
#!/usr/bin/env bash
set -euo pipefail
LOG_DIR="${LOG_DIR:-/opt/backups/yanxitong/ops-logs}"
LOG_RETENTION_DAYS="${LOG_RETENTION_DAYS:-14}"
mkdir -p "$LOG_DIR"
log_file="${LOG_DIR}/ops-check-$(date +%Y%m%d).log"
{
  echo "===== $(date -Iseconds) ops check start ====="
  BASE_URL="${BASE_URL:-https://yxt.yqej.cn}" BACKUP_DIR="${BACKUP_DIR:-/opt/backups/yanxitong/mysql}" MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-26}" DISK_WARN_PERCENT="${DISK_WARN_PERCENT:-80}" DISK_FAIL_PERCENT="${DISK_FAIL_PERCENT:-90}" LOG_SINCE="${LOG_SINCE:-1h}" /opt/apps/yanxitong/ops/ops-check.sh
  echo "===== $(date -Iseconds) ops check ok ====="
} >> "$log_file" 2>&1
find "$LOG_DIR" -type f -name '*.log' -mtime "+${LOG_RETENTION_DAYS}" -delete
OPS_RUNNER

chmod 0755 "${OPS_DIR}/backup-mysql.sh" "${OPS_DIR}/ops-check.sh" "${OPS_DIR}/run-backup-cron.sh" "${OPS_DIR}/run-ops-check-cron.sh"

existing_cron="$(mktemp)"
new_cron="$(mktemp)"
crontab -l 2>/dev/null > "$existing_cron" || true
awk -v begin="$CRON_MARKER_BEGIN" -v end="$CRON_MARKER_END" '
  $0 == begin { skip = 1; next }
  $0 == end { skip = 0; next }
  skip != 1 { print }
' "$existing_cron" > "$new_cron"

cat >> "$new_cron" <<CRON
${CRON_MARKER_BEGIN}
${BACKUP_CRON} OPS_DIR=${OPS_DIR} BACKUP_DIR=${BACKUP_DIR} LOG_DIR=${LOG_DIR} MYSQL_CONTAINER=${MYSQL_CONTAINER} DATABASE=${DATABASE} BACKUP_RETENTION_DAYS=${BACKUP_RETENTION_DAYS} LOG_RETENTION_DAYS=${LOG_RETENTION_DAYS} ${OPS_DIR}/run-backup-cron.sh
${OPS_CHECK_CRON} OPS_DIR=${OPS_DIR} BACKUP_DIR=${BACKUP_DIR} LOG_DIR=${LOG_DIR} BASE_URL=${BASE_URL} MAX_BACKUP_AGE_HOURS=${MAX_BACKUP_AGE_HOURS} DISK_WARN_PERCENT=${DISK_WARN_PERCENT} DISK_FAIL_PERCENT=${DISK_FAIL_PERCENT} LOG_SINCE=${LOG_SINCE} LOG_RETENTION_DAYS=${LOG_RETENTION_DAYS} ${OPS_DIR}/run-ops-check-cron.sh
${CRON_MARKER_END}
CRON

crontab "$new_cron"
rm -f "$existing_cron" "$new_cron"

echo "Installed Yanxitong schedules:"
crontab -l | sed -n "/${CRON_MARKER_BEGIN}/,/${CRON_MARKER_END}/p"
REMOTE
