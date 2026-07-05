#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
WECHAT_CLI="${WECHAT_CLI:-/Applications/wechatwebdevtools.app/Contents/MacOS/cli}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)}"
ARTIFACTS_DIR="${ARTIFACTS_DIR:-${REPO_ROOT}/.artifacts/wechat-preview}"
QR_OUTPUT="${QR_OUTPUT:-${ARTIFACTS_DIR}/latest-miniapp-preview.png}"
INFO_OUTPUT="${INFO_OUTPUT:-${ARTIFACTS_DIR}/latest-miniapp-preview.json}"
PROJECT_DIR="${PROJECT_DIR:-${REPO_ROOT}/miniapp/dist/build/mp-weixin}"
SKIP_BUILD="${SKIP_BUILD:-0}"

log() {
  printf '[miniapp-preview] %s\n' "$*"
}

[[ -x "${WECHAT_CLI}" ]] || {
  echo "WeChat DevTools CLI not found or not executable: ${WECHAT_CLI}" >&2
  exit 1
}

mkdir -p "${ARTIFACTS_DIR}"

log "Running miniapp experience check."
node "${REPO_ROOT}/deploy/scripts/miniapp-experience-check.mjs"

log "Running miniapp route check."
node "${REPO_ROOT}/deploy/scripts/miniapp-route-check.mjs"

log "Running miniapp interaction check."
node "${REPO_ROOT}/deploy/scripts/miniapp-interaction-check.mjs"

if [[ "${SKIP_BUILD}" == "1" ]]; then
  log "Skipping miniapp build."
else
  log "Building miniapp."
  (cd "${REPO_ROOT}/miniapp" && npm run build)
fi

[[ -f "${PROJECT_DIR}/project.config.json" ]] || {
  echo "Miniapp build output missing project.config.json: ${PROJECT_DIR}" >&2
  exit 1
}

log "Generating WeChat preview QR."
"${WECHAT_CLI}" preview \
  --project "${PROJECT_DIR}" \
  --qr-format image \
  --qr-output "${QR_OUTPUT}" \
  --info-output "${INFO_OUTPUT}"

log "Preview QR: ${QR_OUTPUT}"
log "Preview info: ${INFO_OUTPUT}"
log "Run ID: ${RUN_ID}"
