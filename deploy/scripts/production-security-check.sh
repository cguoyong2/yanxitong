#!/usr/bin/env bash
set -euo pipefail

SSH_TARGET="${SSH_TARGET:-root@115.29.229.188}"
BASE_URL="${BASE_URL:-https://yxt.yqej.cn}"
BASE_URL="${BASE_URL%/}"
BACKUP_ROOT="${BACKUP_ROOT:-/opt/backups/yanxitong}"
EDGE_CONF="${EDGE_CONF:-/opt/apps/_edge/conf.d/yanxitong.conf}"

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

headers="$(curl -fsSI "${BASE_URL}/login" | tr -d '\r')"

require_header() {
  local header="$1"
  if printf '%s\n' "$headers" | grep -iq "^${header}:"; then
    pass "header present: ${header}"
  else
    add_failure "missing security header: ${header}"
  fi
}

require_header "Strict-Transport-Security"
require_header "X-Content-Type-Options"
require_header "X-Frame-Options"
require_header "Referrer-Policy"
require_header "Permissions-Policy"
require_header "Content-Security-Policy"

remote_output="$(ssh "${SSH_TARGET}" \
  "BACKUP_ROOT='${BACKUP_ROOT}' EDGE_CONF='${EDGE_CONF}' bash -s" <<'REMOTE'
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

public_listeners="$(ss -lntH | awk 'index($4, "0.0.0.0:") == 1 || index($4, "[::]:") == 1 { print $4 }' | sort -u)"
for required in "0.0.0.0:80" "0.0.0.0:443"; do
  if printf '%s\n' "$public_listeners" | grep -qx "$required"; then
    pass "public listener present: ${required}"
  else
    add_failure "expected public listener missing: ${required}"
  fi
done

unexpected_yxt_ports="$(docker ps --format '{{.Names}} {{.Ports}}' | awk '$1 ~ /^yanxitong-/ && (index($0, "0.0.0.0") || index($0, "[::]")) { print }')"
if [[ -n "$unexpected_yxt_ports" ]]; then
  add_failure "Yanxitong app/data containers have direct public ports: ${unexpected_yxt_ports}"
else
  pass "Yanxitong containers are not directly published except edge proxy"
fi

other_public_ports="$(
  printf '%s\n' "$public_listeners" | awk '
    $0 == "0.0.0.0:80" { next }
    $0 == "0.0.0.0:443" { next }
    $0 == "0.0.0.0:22" { next }
    $0 == "[::]:80" { next }
    $0 == "[::]:443" { next }
    $0 == "[::]:22" { next }
    { print }
  '
)"
if [[ -n "$other_public_ports" ]]; then
  add_warning "other non-Yanxitong public listeners exist on this shared server: $(printf '%s' "$other_public_ports" | tr '\n' ' ')"
else
  pass "no unexpected extra public listeners"
fi

if [[ -f "$EDGE_CONF" ]]; then
  for token in \
    "limit_req_zone" \
    "zone=yxt_public_entry" \
    "zone=yxt_payment_entry" \
    "zone=yxt_login_entry" \
    "Strict-Transport-Security" \
    "Content-Security-Policy" \
    "Permissions-Policy"; do
    if grep -q "$token" "$EDGE_CONF"; then
      pass "edge config contains: ${token}"
    else
      add_failure "edge config missing: ${token}"
    fi
  done
else
  add_failure "edge config not found: ${EDGE_CONF}"
fi

for path in "$BACKUP_ROOT" "$BACKUP_ROOT/mysql" "$BACKUP_ROOT/ops-logs"; do
  if [[ -e "$path" ]]; then
    mode="$(stat -c '%a' "$path")"
    owner="$(stat -c '%U:%G' "$path")"
    if [[ "$mode" == "700" && "$owner" == "root:root" ]]; then
      pass "restricted backup path: ${mode} ${owner} ${path}"
    else
      add_failure "backup path should be 700 root:root, got ${mode} ${owner}: ${path}"
    fi
  else
    add_failure "backup path missing: ${path}"
  fi
done

if docker exec global-edge-nginx nginx -t >/tmp/yanxitong-security-nginx.log 2>&1; then
  pass "global-edge-nginx config syntax"
else
  add_failure "global-edge-nginx config syntax failed: $(tr '\n' ' ' < /tmp/yanxitong-security-nginx.log)"
fi

printf '__REMOTE_SECURITY_SUMMARY__ failures=%s warnings=%s\n' "${#failures[@]}" "${#warnings[@]}"
if [[ "${#failures[@]}" -gt 0 ]]; then
  exit 2
fi
REMOTE
)" || remote_status=$?
remote_status="${remote_status:-0}"

echo "$remote_output"

remote_warnings="$(printf '%s\n' "$remote_output" | sed -n 's/^__REMOTE_SECURITY_SUMMARY__ failures=[0-9][0-9]* warnings=\([0-9][0-9]*\)$/\1/p' | tail -1)"
if [[ -n "$remote_warnings" && "$remote_warnings" -gt 0 ]]; then
  warnings+=("${remote_warnings} remote warning(s)")
fi

if [[ "$remote_status" -ne 0 ]]; then
  add_failure "remote security checks failed"
fi

echo
echo "Production security check summary:"
echo "- failures: ${#failures[@]}"
echo "- warnings: ${#warnings[@]}"
echo "- base URL: ${BASE_URL}"
echo "- ssh target: ${SSH_TARGET}"

if [[ "${#failures[@]}" -gt 0 ]]; then
  exit 1
fi
