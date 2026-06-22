#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
RUN_ID="$(date +%Y%m%d%H%M%S)"
ARTIFACTS_ROOT="${ARTIFACTS_ROOT:-${TMPDIR:-/tmp}/yanxitong-release-readiness-${RUN_ID}}"
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
REQUIRE_READINESS_READY="${REQUIRE_READINESS_READY:-0}"
SKIP_MINIAPP_BUILD="${SKIP_MINIAPP_BUILD:-0}"

mkdir -p "${ARTIFACTS_ROOT}"

log() {
  printf '[release-readiness] %s\n' "$*"
}

run_step() {
  local name="$1"
  shift
  log "Running ${name}."
  set +e
  "$@" >"${ARTIFACTS_ROOT}/${name}.log" 2>&1
  local status=$?
  set -e
  if (( status != 0 )); then
    log "${name} failed. See ${ARTIFACTS_ROOT}/${name}.log"
    return "${status}"
  fi
  log "${name} passed."
}

write_summary() {
  RUN_ID="${RUN_ID}" \
  ARTIFACTS_ROOT="${ARTIFACTS_ROOT}" \
  BASE_URL="${BASE_URL}" \
  OVERALL_STATUS="${OVERALL_STATUS:-failed}" \
  BACKEND_TEST_STATUS="${BACKEND_TEST_STATUS:-not_run}" \
  ADMIN_BUILD_STATUS="${ADMIN_BUILD_STATUS:-not_run}" \
  CONFIRM_SCREEN_BUILD_STATUS="${CONFIRM_SCREEN_BUILD_STATUS:-not_run}" \
  MINIAPP_BUILD_STATUS="${MINIAPP_BUILD_STATUS:-not_run}" \
  READINESS_STATUS="${READINESS_STATUS:-not_checked}" \
  READINESS_HTTP_STATUS="${READINESS_HTTP_STATUS:-}" \
  REQUIRE_READINESS_READY="${REQUIRE_READINESS_READY}" \
  node <<'NODE'
const fs = require('fs');
const path = require('path');

function tail(file, lines = 20) {
  if (!fs.existsSync(file)) {
    return [];
  }
  const content = fs.readFileSync(file, 'utf8').trimEnd();
  return content ? content.split(/\r?\n/).slice(-lines) : [];
}

function readJson(file) {
  if (!fs.existsSync(file)) {
    return null;
  }
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch {
    return null;
  }
}

const artifactsRoot = process.env.ARTIFACTS_ROOT;
const readinessFile = path.join(artifactsRoot, 'readiness.json');
const summary = {
  runId: process.env.RUN_ID,
  status: process.env.OVERALL_STATUS,
  baseUrl: process.env.BASE_URL,
  artifactsRoot,
  requireReadinessReady: process.env.REQUIRE_READINESS_READY === '1',
  steps: {
    backendTests: {
      status: process.env.BACKEND_TEST_STATUS,
      log: path.join(artifactsRoot, 'backend-tests.log'),
      tail: tail(path.join(artifactsRoot, 'backend-tests.log'))
    },
    adminBuild: {
      status: process.env.ADMIN_BUILD_STATUS,
      log: path.join(artifactsRoot, 'admin-build.log'),
      tail: tail(path.join(artifactsRoot, 'admin-build.log'))
    },
    confirmScreenBuild: {
      status: process.env.CONFIRM_SCREEN_BUILD_STATUS,
      log: path.join(artifactsRoot, 'confirm-screen-build.log'),
      tail: tail(path.join(artifactsRoot, 'confirm-screen-build.log'))
    },
    miniappBuild: {
      status: process.env.MINIAPP_BUILD_STATUS,
      log: path.join(artifactsRoot, 'miniapp-build.log'),
      tail: tail(path.join(artifactsRoot, 'miniapp-build.log'))
    },
    securityReadiness: {
      status: process.env.READINESS_STATUS,
      httpStatus: process.env.READINESS_HTTP_STATUS || null,
      response: readJson(readinessFile)
    }
  }
};

fs.writeFileSync(path.join(artifactsRoot, 'summary.json'), JSON.stringify(summary, null, 2));
NODE
}

on_exit() {
  local status=$?
  if [[ "${OVERALL_STATUS:-running}" == "running" ]]; then
    OVERALL_STATUS="failed"
  fi
  write_summary
  if (( status == 0 )); then
    log "Release readiness passed. Summary: ${ARTIFACTS_ROOT}/summary.json"
  else
    log "Release readiness failed. Summary: ${ARTIFACTS_ROOT}/summary.json"
  fi
}
trap on_exit EXIT

OVERALL_STATUS="running"

BACKEND_TEST_STATUS="running"
run_step backend-tests bash -lc "cd '${REPO_ROOT}/server' && mvn -q test"
BACKEND_TEST_STATUS="passed"

ADMIN_BUILD_STATUS="running"
run_step admin-build bash -lc "cd '${REPO_ROOT}/admin' && npm run build"
ADMIN_BUILD_STATUS="passed"

CONFIRM_SCREEN_BUILD_STATUS="running"
run_step confirm-screen-build bash -lc "cd '${REPO_ROOT}/confirm-screen' && npm run build"
CONFIRM_SCREEN_BUILD_STATUS="passed"

if [[ "${SKIP_MINIAPP_BUILD}" == "1" ]]; then
  MINIAPP_BUILD_STATUS="skipped"
  log "Skipping miniapp build."
else
  MINIAPP_BUILD_STATUS="running"
  run_step miniapp-build bash -lc "cd '${REPO_ROOT}/miniapp' && npm run build"
  MINIAPP_BUILD_STATUS="passed"
fi

READINESS_STATUS="not_checked"
READINESS_HTTP_STATUS="$(curl -sS -o "${ARTIFACTS_ROOT}/readiness.json" -w "%{http_code}" "${BASE_URL}/api/health/readiness" 2>/dev/null || true)"
if [[ "${READINESS_HTTP_STATUS}" == "200" ]]; then
  READINESS_STATUS="$(node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); console.log(data.data?.status || 'UNKNOWN')" "${ARTIFACTS_ROOT}/readiness.json")"
  log "Security readiness status: ${READINESS_STATUS}."
  if [[ "${REQUIRE_READINESS_READY}" == "1" && "${READINESS_STATUS}" != "READY" ]]; then
    echo "security readiness must be READY, got ${READINESS_STATUS}" >&2
    exit 1
  fi
else
  READINESS_STATUS="unreachable"
  log "Backend readiness endpoint is unreachable at ${BASE_URL}/api/health/readiness; recorded as not checked."
  if [[ "${REQUIRE_READINESS_READY}" == "1" ]]; then
    echo "security readiness must be reachable and READY" >&2
    exit 1
  fi
fi

OVERALL_STATUS="passed"
write_summary
