#!/usr/bin/env bash
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://127.0.0.1:5173}"

if ! curl -fsS "${ADMIN_URL}/login" >/dev/null; then
  echo "Admin frontend is not reachable at ${ADMIN_URL}." >&2
  echo "Start it first, for example: cd admin && npm run dev -- --host 127.0.0.1 --port 5173" >&2
  exit 1
fi

node deploy/scripts/admin-frontend-smoke.mjs
