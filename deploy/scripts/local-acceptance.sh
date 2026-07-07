#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
RUN_ID="$(date +%Y%m%d%H%M%S)"
ARTIFACTS_ROOT="${ARTIFACTS_ROOT:-${TMPDIR:-/tmp}/yanxitong-local-acceptance-${RUN_ID}}"

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_URL="${ADMIN_URL:-http://127.0.0.1:5173}"
CONFIRM_SCREEN_URL="${CONFIRM_SCREEN_URL:-http://127.0.0.1:5174/confirm-screen}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"

DB_URL="${DB_URL:-jdbc:mysql://127.0.0.1:3308/yanxitong?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}"
DB_USERNAME="${DB_USERNAME:-yanxitong}"
DB_PASSWORD="${DB_PASSWORD:-yanxitong}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6381}"
DB_PORT="${DB_PORT:-$(DB_URL="${DB_URL}" node -e "const url = process.env.DB_URL || ''; const match = url.match(/\\/\\/[^:/?#]+:(\\d+)/); console.log(match ? match[1] : '3308');")}"

STARTED_PIDS=()
OVERALL_STATUS="running"
BACKEND_SMOKE_STATUS="not_run"
NON_PAYMENT_FLOW_STATUS="not_run"
SEED_STATUS="not_run"
ADMIN_SMOKE_STATUS="not_run"
CONFIRM_SCREEN_SMOKE_STATUS="not_run"
PUBLIC_INVITATION_SMOKE_STATUS="not_run"
BACKEND_SMOKE_ARTIFACTS=""
NON_PAYMENT_FLOW_ARTIFACTS=""
SEED_ARTIFACTS=""
ADMIN_SMOKE_ARTIFACTS=""
CONFIRM_SCREEN_SMOKE_ARTIFACTS=""
PUBLIC_INVITATION_SMOKE_ARTIFACTS=""
mkdir -p "${ARTIFACTS_ROOT}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'EOF'
Usage: bash deploy/scripts/local-acceptance.sh

Runs the local Yanxitong MVP acceptance chain:
- start Docker infrastructure when needed
- start backend/admin/confirm-screen when needed
- run backend smoke
- seed demo data
- run admin frontend smoke
- run confirm-screen frontend smoke
- write a consolidated summary.json

Common environment variables:
  ARTIFACTS_ROOT
  BASE_URL
  ADMIN_URL
  CONFIRM_SCREEN_URL
  DB_URL
  DB_PORT
  DB_USERNAME
  DB_PASSWORD
  REDIS_HOST
  REDIS_PORT
  PAYMENT_MOCK_SUCCESS_ENABLED=true is set automatically only when this script starts the backend
  LOCAL_ACCEPTANCE_SKIP_DOCKER=1
EOF
  exit 0
fi

log() {
  printf '[local-acceptance] %s\n' "$*"
}

cleanup() {
  local pid
  for pid in "${STARTED_PIDS[@]:-}"; do
    if kill -0 "${pid}" >/dev/null 2>&1; then
      kill "${pid}" >/dev/null 2>&1 || true
      wait "${pid}" >/dev/null 2>&1 || true
    fi
  done
}
on_exit() {
  local status=$?
  set +e
  if [[ "${OVERALL_STATUS}" == "running" ]]; then
    if (( status == 0 )); then
      OVERALL_STATUS="passed"
    else
      OVERALL_STATUS="failed"
    fi
  fi
  write_summary
  cleanup
  return "${status}"
}
trap on_exit EXIT

health_url() {
  printf '%s/api/health' "${BASE_URL}"
}

port_listening() {
  local port="$1"
  lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}

wait_for_url() {
  local name="$1"
  local url="$2"
  local max_seconds="${3:-60}"
  local elapsed=0
  until curl -fsS "${url}" >/dev/null 2>&1; do
    if (( elapsed >= max_seconds )); then
      echo "${name} is not reachable at ${url}. See logs in ${ARTIFACTS_ROOT}." >&2
      return 1
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done
}

start_infra() {
  if [[ "${LOCAL_ACCEPTANCE_SKIP_DOCKER:-0}" == "1" ]]; then
    log "Skipping docker compose startup."
    return
  fi
  if ! command -v docker >/dev/null 2>&1; then
    log "Docker is not available; assuming MySQL and Redis are already running."
    return
  fi
  if port_listening "${DB_PORT}" && port_listening "${REDIS_PORT}"; then
    log "MySQL port ${DB_PORT} and Redis port ${REDIS_PORT} are already listening; skipping docker compose startup."
    return
  fi
  log "Starting MySQL and Redis with deploy/docker-compose.yml."
  (cd "${REPO_ROOT}" && docker compose -f deploy/docker-compose.yml up -d)
}

start_backend_if_needed() {
  if curl -fsS "$(health_url)" >/dev/null 2>&1; then
    log "Backend already reachable at ${BASE_URL}; reusing it."
    return
  fi

  log "Backend is not reachable; building and starting server."
  (cd "${REPO_ROOT}/server" && mvn -q clean package -DskipTests)

  (
    cd "${REPO_ROOT}/server"
    DB_URL="${DB_URL}" \
    DB_USERNAME="${DB_USERNAME}" \
    DB_PASSWORD="${DB_PASSWORD}" \
    REDIS_HOST="${REDIS_HOST}" \
    REDIS_PORT="${REDIS_PORT}" \
    PAYMENT_MOCK_SUCCESS_ENABLED=true \
    java -jar target/server-0.0.1-SNAPSHOT.jar
  ) >"${ARTIFACTS_ROOT}/server.log" 2>&1 &
  STARTED_PIDS+=("$!")

  wait_for_url "Backend" "$(health_url)" 90
}

start_admin_if_needed() {
  if curl -fsS "${ADMIN_URL}/login" >/dev/null 2>&1; then
    log "Admin frontend already reachable at ${ADMIN_URL}; reusing it."
    return
  fi

  log "Admin frontend is not reachable; starting Vite dev server."
  (
    cd "${REPO_ROOT}/admin"
    npm run dev -- --host 127.0.0.1 --port 5173
  ) >"${ARTIFACTS_ROOT}/admin.log" 2>&1 &
  STARTED_PIDS+=("$!")

  wait_for_url "Admin frontend" "${ADMIN_URL}/login" 60
}

start_confirm_screen_if_needed() {
  if curl -fsS "${CONFIRM_SCREEN_URL}/bind" >/dev/null 2>&1; then
    log "Confirm-screen frontend already reachable at ${CONFIRM_SCREEN_URL}; reusing it."
    return
  fi

  log "Confirm-screen frontend is not reachable; starting Vite dev server."
  (
    cd "${REPO_ROOT}/confirm-screen"
    npm run dev -- --host 127.0.0.1 --port 5174
  ) >"${ARTIFACTS_ROOT}/confirm-screen.log" 2>&1 &
  STARTED_PIDS+=("$!")

  wait_for_url "Confirm-screen frontend" "${CONFIRM_SCREEN_URL}/bind" 60
}

seed_demo_data() {
  local seed_log="${ARTIFACTS_ROOT}/seed-demo-data.log"
  log "Seeding demo data for frontend acceptance."
  SEED_STATUS="running"
  set +e
  (
    cd "${REPO_ROOT}"
    DEMO_KEY="${DEMO_KEY:-local-acceptance}" \
    BASE_URL="${BASE_URL}" \
    ADMIN_USERNAME="${ADMIN_USERNAME}" \
    ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
    bash deploy/scripts/seed-demo-data.sh
  ) | tee "${seed_log}"
  local status=${PIPESTATUS[0]}
  set -e
  if (( status != 0 )); then
    SEED_STATUS="failed"
    return "${status}"
  fi

  local seed_artifacts
  seed_artifacts="$(awk -F': ' '/^Artifacts:/ { print $2 }' "${seed_log}" | tail -n 1)"
  if [[ -z "${seed_artifacts}" || ! -f "${seed_artifacts}/summary.json" ]]; then
    echo "Unable to locate seeded demo summary." >&2
    SEED_STATUS="failed"
    exit 1
  fi
  SEED_ARTIFACTS="${seed_artifacts}"

  BANQUET_ID="$(node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); console.log(data.banquetId)" "${seed_artifacts}/summary.json")"
  CONFIRM_SCREEN_BIND_CODE="$(node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); console.log(data.confirmScreenBindCode)" "${seed_artifacts}/summary.json")"
  SHARE_SLUG="$(node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); console.log(data.shareSlug)" "${seed_artifacts}/summary.json")"
  export BANQUET_ID CONFIRM_SCREEN_BIND_CODE SHARE_SLUG

  node -e "const fs=require('fs'); const src=process.argv[1]; const dest=process.argv[2]; fs.copyFileSync(src, dest);" \
    "${seed_artifacts}/summary.json" \
    "${ARTIFACTS_ROOT}/seed-summary.json"
  SEED_STATUS="passed"
}

run_public_invitation_smoke() {
  log "Running public invitation smoke."
  PUBLIC_INVITATION_SMOKE_STATUS="running"
  set +e
  (
    cd "${REPO_ROOT}"
    BASE_URL="${BASE_URL}" \
    SHARE_SLUG="${SHARE_SLUG}" \
    node deploy/scripts/public-invitation-smoke.mjs
  ) | tee "${ARTIFACTS_ROOT}/public-invitation-smoke.log"
  local status=${PIPESTATUS[0]}
  set -e
  PUBLIC_INVITATION_SMOKE_ARTIFACTS="$(awk -F'Artifacts: ' '/Public invitation smoke (passed|failed)\. Artifacts:/ { print $2 }' "${ARTIFACTS_ROOT}/public-invitation-smoke.log" | tail -n 1)"
  if (( status != 0 )); then
    PUBLIC_INVITATION_SMOKE_STATUS="failed"
    return "${status}"
  fi
  PUBLIC_INVITATION_SMOKE_STATUS="passed"
}

run_backend_smoke() {
  log "Running backend MVP smoke."
  BACKEND_SMOKE_STATUS="running"
  set +e
  (
    cd "${REPO_ROOT}"
    BASE_URL="${BASE_URL}" \
    ADMIN_USERNAME="${ADMIN_USERNAME}" \
    ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
    bash deploy/scripts/smoke-test.sh
  ) | tee "${ARTIFACTS_ROOT}/backend-smoke.log"
  local status=${PIPESTATUS[0]}
  set -e
  BACKEND_SMOKE_ARTIFACTS="$(awk -F': ' '/^Smoke test passed\. Artifacts:/ { print $2 } /^Artifacts:/ { print $2 }' "${ARTIFACTS_ROOT}/backend-smoke.log" | tail -n 1)"
  if (( status != 0 )); then
    BACKEND_SMOKE_STATUS="failed"
    return "${status}"
  fi
  BACKEND_SMOKE_STATUS="passed"
}

run_non_payment_flow() {
  log "Running non-payment MVP API acceptance."
  NON_PAYMENT_FLOW_STATUS="running"
  set +e
  (
    cd "${REPO_ROOT}"
    BASE_URL="${BASE_URL}" \
    ADMIN_USERNAME="${ADMIN_USERNAME}" \
    ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
    bash deploy/scripts/non-payment-flow-acceptance.sh
  ) | tee "${ARTIFACTS_ROOT}/non-payment-flow.log"
  local status=${PIPESTATUS[0]}
  set -e
  NON_PAYMENT_FLOW_ARTIFACTS="$(awk -F': ' '/^\[non-payment-flow\] passed\. Artifacts:/ { print $2 } /^Artifacts:/ { print $2 }' "${ARTIFACTS_ROOT}/non-payment-flow.log" | tail -n 1)"
  if (( status != 0 )); then
    NON_PAYMENT_FLOW_STATUS="failed"
    return "${status}"
  fi
  NON_PAYMENT_FLOW_STATUS="passed"
}

run_admin_smoke() {
  log "Running admin frontend smoke."
  ADMIN_SMOKE_STATUS="running"
  set +e
  (
    cd "${REPO_ROOT}"
    BASE_URL="${BASE_URL}" \
    ADMIN_URL="${ADMIN_URL}" \
    ADMIN_USERNAME="${ADMIN_USERNAME}" \
    ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
    BANQUET_ID="${BANQUET_ID}" \
    bash deploy/scripts/admin-frontend-smoke.sh
  ) | tee "${ARTIFACTS_ROOT}/admin-smoke.log"
  local status=${PIPESTATUS[0]}
  set -e
  ADMIN_SMOKE_ARTIFACTS="$(awk -F'Artifacts: ' '/Admin frontend smoke (passed|failed)\. Artifacts:/ { print $2 }' "${ARTIFACTS_ROOT}/admin-smoke.log" | tail -n 1)"
  if (( status != 0 )); then
    ADMIN_SMOKE_STATUS="failed"
    return "${status}"
  fi
  ADMIN_SMOKE_STATUS="passed"
}

run_confirm_screen_smoke() {
  log "Running confirm-screen frontend smoke."
  CONFIRM_SCREEN_SMOKE_STATUS="running"
  set +e
  (
    cd "${REPO_ROOT}"
    BASE_URL="${BASE_URL}" \
    CONFIRM_SCREEN_URL="${CONFIRM_SCREEN_URL}" \
    BANQUET_ID="${BANQUET_ID}" \
    CONFIRM_SCREEN_BIND_CODE="${CONFIRM_SCREEN_BIND_CODE}" \
    bash deploy/scripts/confirm-screen-smoke.sh
  ) | tee "${ARTIFACTS_ROOT}/confirm-screen-smoke.log"
  local status=${PIPESTATUS[0]}
  set -e
  CONFIRM_SCREEN_SMOKE_ARTIFACTS="$(awk -F'Artifacts: ' '/Confirm-screen smoke (passed|failed)\. Artifacts:/ { print $2 }' "${ARTIFACTS_ROOT}/confirm-screen-smoke.log" | tail -n 1)"
  if (( status != 0 )); then
    CONFIRM_SCREEN_SMOKE_STATUS="failed"
    return "${status}"
  fi
  CONFIRM_SCREEN_SMOKE_STATUS="passed"
}

write_summary() {
  RUN_ID="${RUN_ID}" \
  OVERALL_STATUS="${OVERALL_STATUS}" \
  BASE_URL="${BASE_URL}" \
  ADMIN_URL="${ADMIN_URL}" \
  CONFIRM_SCREEN_URL="${CONFIRM_SCREEN_URL}" \
  BANQUET_ID="${BANQUET_ID:-}" \
  CONFIRM_SCREEN_BIND_CODE="${CONFIRM_SCREEN_BIND_CODE:-}" \
  ARTIFACTS_ROOT="${ARTIFACTS_ROOT}" \
  BACKEND_SMOKE_STATUS="${BACKEND_SMOKE_STATUS}" \
  BACKEND_SMOKE_ARTIFACTS="${BACKEND_SMOKE_ARTIFACTS}" \
  NON_PAYMENT_FLOW_STATUS="${NON_PAYMENT_FLOW_STATUS}" \
  NON_PAYMENT_FLOW_ARTIFACTS="${NON_PAYMENT_FLOW_ARTIFACTS}" \
  SEED_STATUS="${SEED_STATUS}" \
  SEED_ARTIFACTS="${SEED_ARTIFACTS}" \
  ADMIN_SMOKE_STATUS="${ADMIN_SMOKE_STATUS}" \
  ADMIN_SMOKE_ARTIFACTS="${ADMIN_SMOKE_ARTIFACTS}" \
  CONFIRM_SCREEN_SMOKE_STATUS="${CONFIRM_SCREEN_SMOKE_STATUS}" \
  CONFIRM_SCREEN_SMOKE_ARTIFACTS="${CONFIRM_SCREEN_SMOKE_ARTIFACTS}" \
  PUBLIC_INVITATION_SMOKE_STATUS="${PUBLIC_INVITATION_SMOKE_STATUS}" \
  PUBLIC_INVITATION_SMOKE_ARTIFACTS="${PUBLIC_INVITATION_SMOKE_ARTIFACTS}" \
  node <<'NODE'
const fs = require('fs');
const path = require('path');

function readJson(file) {
  if (!file || !fs.existsSync(file)) {
    return null;
  }
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch {
    return null;
  }
}

function tail(file, lines = 20) {
  if (!file || !fs.existsSync(file)) {
    return [];
  }
  const content = fs.readFileSync(file, 'utf8').trimEnd();
  if (!content) {
    return [];
  }
  return content.split(/\r?\n/).slice(-lines);
}

function summaryPath(dir) {
  return dir ? path.join(dir, 'summary.json') : '';
}

function screenshotFiles(dir) {
  if (!dir || !fs.existsSync(dir)) {
    return [];
  }
  return fs.readdirSync(dir)
    .filter((name) => name.endsWith('.png'))
    .sort()
    .map((name) => path.join(dir, name));
}

const artifactsRoot = process.env.ARTIFACTS_ROOT;
const backendLog = path.join(artifactsRoot, 'backend-smoke.log');
const nonPaymentFlowLog = path.join(artifactsRoot, 'non-payment-flow.log');
const seedLog = path.join(artifactsRoot, 'seed-demo-data.log');
const adminLog = path.join(artifactsRoot, 'admin-smoke.log');
const confirmScreenLog = path.join(artifactsRoot, 'confirm-screen-smoke.log');
const publicInvitationLog = path.join(artifactsRoot, 'public-invitation-smoke.log');

const adminSummary = readJson(summaryPath(process.env.ADMIN_SMOKE_ARTIFACTS));
const confirmScreenSummary = readJson(summaryPath(process.env.CONFIRM_SCREEN_SMOKE_ARTIFACTS));
const publicInvitationSummary = readJson(summaryPath(process.env.PUBLIC_INVITATION_SMOKE_ARTIFACTS));
const seedSummary = readJson(path.join(artifactsRoot, 'seed-summary.json')) ||
  readJson(summaryPath(process.env.SEED_ARTIFACTS));
const adminScreenshots = adminSummary?.screenshots || screenshotFiles(process.env.ADMIN_SMOKE_ARTIFACTS);
const confirmScreenScreenshots = screenshotFiles(process.env.CONFIRM_SCREEN_SMOKE_ARTIFACTS);
const publicInvitationScreenshots = publicInvitationSummary?.screenshots || screenshotFiles(process.env.PUBLIC_INVITATION_SMOKE_ARTIFACTS);
const visualManifest = {
  generatedAt: new Date().toISOString(),
  artifactsRoot,
  admin: {
    artifactsDir: process.env.ADMIN_SMOKE_ARTIFACTS || null,
    screenshots: adminScreenshots
  },
  confirmScreen: {
    artifactsDir: process.env.CONFIRM_SCREEN_SMOKE_ARTIFACTS || null,
    screenshots: confirmScreenScreenshots
  },
  publicInvitation: {
    artifactsDir: process.env.PUBLIC_INVITATION_SMOKE_ARTIFACTS || null,
    screenshots: publicInvitationScreenshots
  }
};
const visualManifestPath = path.join(artifactsRoot, 'visual-manifest.json');
fs.writeFileSync(visualManifestPath, JSON.stringify(visualManifest, null, 2));

const summary = {
  runId: process.env.RUN_ID,
  status: process.env.OVERALL_STATUS,
  baseUrl: process.env.BASE_URL,
  adminUrl: process.env.ADMIN_URL,
  confirmScreenUrl: process.env.CONFIRM_SCREEN_URL,
  banquetId: process.env.BANQUET_ID || null,
  confirmScreenBindCode: process.env.CONFIRM_SCREEN_BIND_CODE || null,
  artifactsRoot,
  visualManifest: visualManifestPath,
  steps: {
    backendSmoke: {
      status: process.env.BACKEND_SMOKE_STATUS,
      log: backendLog,
      artifactsDir: process.env.BACKEND_SMOKE_ARTIFACTS || null,
      tail: tail(backendLog)
    },
    seedDemoData: {
      status: process.env.SEED_STATUS,
      log: seedLog,
      artifactsDir: process.env.SEED_ARTIFACTS || null,
      summary: seedSummary,
      tail: tail(seedLog)
    },
    nonPaymentFlow: {
      status: process.env.NON_PAYMENT_FLOW_STATUS,
      log: nonPaymentFlowLog,
      artifactsDir: process.env.NON_PAYMENT_FLOW_ARTIFACTS || null,
      summary: readJson(summaryPath(process.env.NON_PAYMENT_FLOW_ARTIFACTS)),
      tail: tail(nonPaymentFlowLog)
    },
    adminSmoke: {
      status: process.env.ADMIN_SMOKE_STATUS,
      log: adminLog,
      artifactsDir: process.env.ADMIN_SMOKE_ARTIFACTS || null,
      summary: adminSummary,
      failedRoutes: adminSummary?.results?.filter((item) => !item.ok) || [],
      screenshots: adminScreenshots,
      tail: tail(adminLog)
    },
    publicInvitationSmoke: {
      status: process.env.PUBLIC_INVITATION_SMOKE_STATUS,
      log: publicInvitationLog,
      artifactsDir: process.env.PUBLIC_INVITATION_SMOKE_ARTIFACTS || null,
      summary: publicInvitationSummary,
      failedSteps: publicInvitationSummary?.results?.filter((item) => !item.ok) || [],
      screenshots: publicInvitationScreenshots,
      tail: tail(publicInvitationLog)
    },
    confirmScreenSmoke: {
      status: process.env.CONFIRM_SCREEN_SMOKE_STATUS,
      log: confirmScreenLog,
      artifactsDir: process.env.CONFIRM_SCREEN_SMOKE_ARTIFACTS || null,
      summary: confirmScreenSummary,
      failedSteps: confirmScreenSummary?.results?.filter((item) => !item.ok) || [],
      screenshots: confirmScreenScreenshots,
      tail: tail(confirmScreenLog)
    }
  }
};

fs.writeFileSync(path.join(artifactsRoot, 'summary.json'), JSON.stringify(summary, null, 2));
NODE
}

main() {
  log "Artifacts: ${ARTIFACTS_ROOT}"
  start_infra
  start_backend_if_needed
  start_admin_if_needed
  start_confirm_screen_if_needed
  run_backend_smoke
  run_non_payment_flow
  seed_demo_data
  run_public_invitation_smoke
  run_admin_smoke
  run_confirm_screen_smoke
  OVERALL_STATUS="passed"
  write_summary
  log "Local acceptance passed. Summary: ${ARTIFACTS_ROOT}/summary.json"
}

main "$@"
