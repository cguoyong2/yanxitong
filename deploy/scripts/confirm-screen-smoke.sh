#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
CONFIRM_SCREEN_URL="${CONFIRM_SCREEN_URL:-http://127.0.0.1:5174}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! curl -fsS "${BASE_URL}/api/health" >/dev/null; then
  echo "Backend is not reachable at ${BASE_URL}." >&2
  echo "Start it first, for example: cd server && mvn spring-boot:run" >&2
  exit 1
fi

if ! curl -fsS "${CONFIRM_SCREEN_URL}/bind" >/dev/null; then
  echo "Confirm-screen frontend is not reachable at ${CONFIRM_SCREEN_URL}." >&2
  echo "Start it first, for example: cd confirm-screen && npm run dev -- --host 127.0.0.1 --port 5174" >&2
  exit 1
fi

if [[ -z "${BANQUET_ID:-}" || -z "${CONFIRM_SCREEN_BIND_CODE:-}" ]]; then
  SEED_LOG="$(mktemp "${TMPDIR:-/tmp}/yanxitong-confirm-screen-seed.XXXXXX")"
  DEMO_KEY="${DEMO_KEY:-confirm-screen-smoke}" BASE_URL="${BASE_URL}" bash "${SCRIPT_DIR}/seed-demo-data.sh" | tee "${SEED_LOG}"
  SEED_ARTIFACTS="$(awk -F': ' '/^Artifacts:/ { print $2 }' "${SEED_LOG}" | tail -n 1)"
  if [[ -z "${SEED_ARTIFACTS}" || ! -f "${SEED_ARTIFACTS}/summary.json" ]]; then
    echo "Unable to read seeded demo summary." >&2
    exit 1
  fi
  BANQUET_ID="$(node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); console.log(data.banquetId)" "${SEED_ARTIFACTS}/summary.json")"
  CONFIRM_SCREEN_BIND_CODE="$(node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); console.log(data.confirmScreenBindCode)" "${SEED_ARTIFACTS}/summary.json")"
  export BANQUET_ID CONFIRM_SCREEN_BIND_CODE
fi

CONFIRM_SCREEN_URL="${CONFIRM_SCREEN_URL}" \
BANQUET_ID="${BANQUET_ID}" \
CONFIRM_SCREEN_BIND_CODE="${CONFIRM_SCREEN_BIND_CODE}" \
node "${SCRIPT_DIR}/confirm-screen-smoke.mjs"
