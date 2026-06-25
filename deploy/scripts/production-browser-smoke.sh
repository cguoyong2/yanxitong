#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://yxt.yqej.cn}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -z "${ADMIN_PASSWORD:-}" ]]; then
  echo "ADMIN_PASSWORD is required." >&2
  exit 1
fi

if ! curl -fsS "${BASE_URL%/}/login" >/dev/null; then
  echo "Admin frontend is not reachable at ${BASE_URL%/}/login." >&2
  exit 1
fi

BASE_URL="${BASE_URL}" node "${SCRIPT_DIR}/production-browser-smoke.mjs"
