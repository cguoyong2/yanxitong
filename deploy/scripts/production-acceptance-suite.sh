#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)}"
ARTIFACTS_ROOT="${ARTIFACTS_ROOT:-${REPO_ROOT}/.artifacts/production-acceptance/${RUN_ID}}"
BASE_URL="${BASE_URL:-https://yxt.yqej.cn}"
ENV_FILE="${ENV_FILE:-${REPO_ROOT}/deploy/.env.production}"

RUN_PREFLIGHT="${RUN_PREFLIGHT:-1}"
RUN_RELEASE_READINESS="${RUN_RELEASE_READINESS:-1}"
RUN_ADMIN_SOURCE_SMOKE="${RUN_ADMIN_SOURCE_SMOKE:-1}"
RUN_PRODUCTION_API="${RUN_PRODUCTION_API:-0}"
RUN_PRODUCTION_BROWSER="${RUN_PRODUCTION_BROWSER:-0}"
RUN_OPS_CHECK="${RUN_OPS_CHECK:-0}"
RUN_SECURITY_CHECK="${RUN_SECURITY_CHECK:-0}"
RUN_MINIAPP_PREVIEW="${RUN_MINIAPP_PREVIEW:-0}"

SKIP_REMOTE_CHECKS="${SKIP_REMOTE_CHECKS:-1}"
REQUIRE_READINESS_READY="${REQUIRE_READINESS_READY:-0}"
SKIP_MINIAPP_BUILD="${SKIP_MINIAPP_BUILD:-0}"

mkdir -p "${ARTIFACTS_ROOT}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'EOF'
Usage:
  bash deploy/scripts/production-acceptance-suite.sh

Default behavior:
  Runs local production preflight, local release-readiness build/test checks,
  and admin source smoke. Remote production checks are opt-in.

Common production run:
  ADMIN_PASSWORD='<admin-password>' \
  BASE_URL=https://yxt.yqej.cn \
  SKIP_REMOTE_CHECKS=0 \
  RUN_PRODUCTION_API=1 \
  RUN_PRODUCTION_BROWSER=1 \
  RUN_OPS_CHECK=1 \
  bash deploy/scripts/production-acceptance-suite.sh

Environment:
  ARTIFACTS_ROOT              output directory
  BASE_URL                    public deployment base URL
  ENV_FILE                    production env file, default deploy/.env.production
  RUN_PREFLIGHT               default 1
  RUN_RELEASE_READINESS       default 1
  RUN_ADMIN_SOURCE_SMOKE      default 1
  RUN_PRODUCTION_API          default 0, requires ADMIN_PASSWORD
  RUN_PRODUCTION_BROWSER      default 0, requires ADMIN_PASSWORD and Chrome
  RUN_OPS_CHECK               default 0, requires SSH access
  RUN_SECURITY_CHECK          default 0, requires SSH access
  RUN_MINIAPP_PREVIEW         default 0, requires WeChat DevTools CLI login
  SKIP_REMOTE_CHECKS          passed to production-preflight, default 1
  REQUIRE_READINESS_READY     default 0 before real WeChat payment launch
  SKIP_MINIAPP_BUILD          passed to release-readiness, default 0
EOF
  exit 0
fi

log() {
  printf '[production-acceptance-suite] %s\n' "$*"
}

run_step() {
  local name="$1"
  shift
  local log_file="${ARTIFACTS_ROOT}/${name}.log"
  log "Running ${name}."
  set +e
  "$@" >"${log_file}" 2>&1
  local status=$?
  set -e
  if (( status == 0 )); then
    log "${name} passed."
    STEP_RESULTS+=("${name}:passed:${log_file}")
  else
    log "${name} failed. See ${log_file}"
    STEP_RESULTS+=("${name}:failed:${log_file}")
    FAILED_STEPS+=("${name}")
  fi
}

skip_step() {
  local name="$1"
  local reason="$2"
  log "Skipping ${name}: ${reason}"
  STEP_RESULTS+=("${name}:skipped:${reason}")
}

write_summary() {
  SUMMARY_FILE="${ARTIFACTS_ROOT}/summary.json" \
  RUN_ID="${RUN_ID}" \
  BASE_URL="${BASE_URL}" \
  ENV_FILE="${ENV_FILE}" \
  OVERALL_STATUS="$([[ "${#FAILED_STEPS[@]}" -eq 0 ]] && echo passed || echo failed)" \
  STEP_RESULTS_TEXT="$(printf '%s\n' "${STEP_RESULTS[@]}")" \
  node <<'NODE'
const fs = require('fs');

function tail(file, lines = 30) {
  if (!file || !fs.existsSync(file)) {
    return [];
  }
  const content = fs.readFileSync(file, 'utf8').trimEnd();
  return content ? content.split(/\r?\n/).slice(-lines) : [];
}

const steps = (process.env.STEP_RESULTS_TEXT || '')
  .split(/\r?\n/)
  .filter(Boolean)
  .map((line) => {
    const [name, status, ...rest] = line.split(':');
    const detail = rest.join(':');
    const step = { name, status };
    if (status === 'skipped') {
      step.reason = detail;
    } else {
      step.log = detail;
      step.tail = tail(detail);
    }
    return step;
  });

const summary = {
  runId: process.env.RUN_ID,
  status: process.env.OVERALL_STATUS,
  baseUrl: process.env.BASE_URL,
  envFile: process.env.ENV_FILE,
  artifactsRoot: process.env.SUMMARY_FILE.replace(/\/summary\.json$/, ''),
  steps
};

fs.writeFileSync(process.env.SUMMARY_FILE, JSON.stringify(summary, null, 2));
NODE
}

STEP_RESULTS=()
FAILED_STEPS=()

if [[ "${RUN_PREFLIGHT}" == "1" ]]; then
  run_step production-preflight env \
    ENV_FILE="${ENV_FILE}" \
    BASE_URL="${BASE_URL}" \
    SKIP_REMOTE_CHECKS="${SKIP_REMOTE_CHECKS}" \
    REQUIRE_READINESS_READY="${REQUIRE_READINESS_READY}" \
    bash "${SCRIPT_DIR}/production-preflight.sh"
else
  skip_step production-preflight "RUN_PREFLIGHT=0"
fi

if [[ "${RUN_RELEASE_READINESS}" == "1" ]]; then
  run_step release-readiness env \
    ARTIFACTS_ROOT="${ARTIFACTS_ROOT}/release-readiness" \
    BASE_URL="${BASE_URL}" \
    REQUIRE_READINESS_READY="${REQUIRE_READINESS_READY}" \
    SKIP_MINIAPP_BUILD="${SKIP_MINIAPP_BUILD}" \
    bash "${SCRIPT_DIR}/release-readiness.sh"
else
  skip_step release-readiness "RUN_RELEASE_READINESS=0"
fi

if [[ "${RUN_ADMIN_SOURCE_SMOKE}" == "1" ]]; then
  run_step admin-source-smoke env \
    ADMIN_SOURCE_ONLY=1 \
    node "${SCRIPT_DIR}/admin-frontend-smoke.mjs"
else
  skip_step admin-source-smoke "RUN_ADMIN_SOURCE_SMOKE=0"
fi

if [[ "${RUN_PRODUCTION_API}" == "1" ]]; then
  if [[ -z "${ADMIN_PASSWORD:-}" ]]; then
    skip_step production-api-acceptance "ADMIN_PASSWORD is required"
    FAILED_STEPS+=("production-api-acceptance")
  else
    run_step production-api-acceptance env \
      ARTIFACTS_DIR="${ARTIFACTS_ROOT}/production-api" \
      BASE_URL="${BASE_URL}" \
      ADMIN_USERNAME="${ADMIN_USERNAME:-admin}" \
      ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
      bash "${SCRIPT_DIR}/production-api-acceptance.sh"
  fi
else
  skip_step production-api-acceptance "RUN_PRODUCTION_API=0"
fi

if [[ "${RUN_PRODUCTION_BROWSER}" == "1" ]]; then
  if [[ -z "${ADMIN_PASSWORD:-}" ]]; then
    skip_step production-browser-smoke "ADMIN_PASSWORD is required"
    FAILED_STEPS+=("production-browser-smoke")
  else
    run_step production-browser-smoke env \
      ARTIFACTS_DIR="${ARTIFACTS_ROOT}/production-browser" \
      BASE_URL="${BASE_URL}" \
      ADMIN_USERNAME="${ADMIN_USERNAME:-admin}" \
      ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
      SHARE_SLUG="${SHARE_SLUG:-}" \
      bash "${SCRIPT_DIR}/production-browser-smoke.sh"
  fi
else
  skip_step production-browser-smoke "RUN_PRODUCTION_BROWSER=0"
fi

if [[ "${RUN_OPS_CHECK}" == "1" ]]; then
  run_step production-ops-check env \
    BASE_URL="${BASE_URL}" \
    REQUIRE_READINESS_READY="${REQUIRE_READINESS_READY}" \
    SSH_TARGET="${SSH_TARGET:-root@115.29.229.188}" \
    bash "${SCRIPT_DIR}/production-ops-check.sh"
else
  skip_step production-ops-check "RUN_OPS_CHECK=0"
fi

if [[ "${RUN_SECURITY_CHECK}" == "1" ]]; then
  run_step production-security-check env \
    BASE_URL="${BASE_URL}" \
    SSH_TARGET="${SSH_TARGET:-root@115.29.229.188}" \
    bash "${SCRIPT_DIR}/production-security-check.sh"
else
  skip_step production-security-check "RUN_SECURITY_CHECK=0"
fi

if [[ "${RUN_MINIAPP_PREVIEW}" == "1" ]]; then
  run_step miniapp-preview env \
    ARTIFACTS_DIR="${ARTIFACTS_ROOT}/miniapp-preview" \
    QR_OUTPUT="${ARTIFACTS_ROOT}/miniapp-preview/latest-miniapp-preview.png" \
    INFO_OUTPUT="${ARTIFACTS_ROOT}/miniapp-preview/latest-miniapp-preview.json" \
    bash "${SCRIPT_DIR}/miniapp-preview.sh"
else
  skip_step miniapp-preview "RUN_MINIAPP_PREVIEW=0"
fi

write_summary

if [[ "${#FAILED_STEPS[@]}" -gt 0 ]]; then
  log "Production acceptance suite failed. Summary: ${ARTIFACTS_ROOT}/summary.json"
  printf '[production-acceptance-suite] Failed steps: %s\n' "${FAILED_STEPS[*]}" >&2
  exit 1
fi

log "Production acceptance suite passed. Summary: ${ARTIFACTS_ROOT}/summary.json"
