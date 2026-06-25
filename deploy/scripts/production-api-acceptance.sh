#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://yxt.yqej.cn}"
BASE_URL="${BASE_URL%/}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
RUN_ID="$(date +%Y%m%d%H%M%S)"
WORK_DIR="${ARTIFACTS_DIR:-${TMPDIR:-/tmp}/yanxitong-production-api-acceptance-${RUN_ID}}"
mkdir -p "$WORK_DIR"

if [[ -z "${ADMIN_PASSWORD:-}" ]]; then
  echo "ADMIN_PASSWORD is required." >&2
  exit 1
fi

request() {
  local name="$1"
  shift
  curl -fsS "$@" > "${WORK_DIR}/${name}.json"
}

request_status() {
  local name="$1"
  shift
  curl -sS -o "${WORK_DIR}/${name}.json" -w "%{http_code}" "$@"
}

json_get() {
  local file="$1"
  local expr="$2"
  node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); console.log(${expr});" "${WORK_DIR}/${file}.json"
}

assert_json() {
  local file="$1"
  local expr="$2"
  local message="$3"
  node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); if (!(${expr})) { throw new Error(process.argv[2]); }" "${WORK_DIR}/${file}.json" "$message"
}

json_escape() {
  node -e "process.stdout.write(JSON.stringify(process.argv[1]))" "$1"
}

url_encode() {
  node -e "process.stdout.write(encodeURIComponent(process.argv[1]))" "$1"
}

AUTH_HEADER=()

request health "${BASE_URL}/api/health"
assert_json health "data.code === 0 && data.data.status === 'UP'" "health check failed"

request readiness "${BASE_URL}/api/health/readiness"
assert_json readiness "data.code === 0 && ['READY', 'WARN', 'BLOCKED'].includes(data.data.status)" "readiness check failed"
READINESS_STATUS="$(json_get readiness "data.data.status")"

request login -X POST "${BASE_URL}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":$(json_escape "${ADMIN_USERNAME}"),\"password\":$(json_escape "${ADMIN_PASSWORD}")}"
TOKEN="$(json_get login "data.data.token")"
AUTH_HEADER=(-H "Authorization: Bearer ${TOKEN}")

request admin_config "${BASE_URL}/api/admin/config-items" "${AUTH_HEADER[@]}"
assert_json admin_config "data.code === 0 && Array.isArray(data.data)" "admin config list failed"

request meta_event_types "${BASE_URL}/api/meta/event-types"
assert_json meta_event_types "data.code === 0 && data.data.length > 0" "event type meta missing"
EVENT_TYPE_CODE="$(json_get meta_event_types "data.data.find((item) => item.enabled !== 0)?.eventTypeCode || data.data[0].eventTypeCode")"

request meta_templates "${BASE_URL}/api/meta/invitation-templates"
assert_json meta_templates "data.code === 0 && data.data.length > 0" "invitation template meta missing"
TEMPLATE_ID="$(json_get meta_templates "data.data[0].id")"

BANQUET_NAME="生产验收宴席 ${RUN_ID}"
HOST_NAME="生产验收主办人 ${RUN_ID}"
RSVP_GUEST="生产验收到宾 ${RUN_ID}"
CASH_GUEST="生产验收现金 ${RUN_ID}"
ONLINE_GUEST="生产验收支付 ${RUN_ID}"
CONFIRM_BIND_CODE="PROD-CS-${RUN_ID}"

request banquet_create -X POST "${BASE_URL}/api/banquets" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":$(json_escape "${BANQUET_NAME}"),\"eventTypeCode\":$(json_escape "${EVENT_TYPE_CODE}"),\"banquetTime\":\"2026-10-01T18:00:00\",\"location\":\"生产验收酒店\",\"templateId\":${TEMPLATE_ID}}"
BANQUET_ID="$(json_get banquet_create "data.data.banquet.id")"
INVITATION_ID="$(json_get banquet_create "data.data.invitation.id")"
SHARE_SLUG="$(json_get banquet_create "data.data.invitation.shareSlug")"

request invitation_update -X PUT "${BASE_URL}/api/invitations/${INVITATION_ID}/basic" \
  -H 'Content-Type: application/json' \
  -d "{\"title\":$(json_escape "生产验收请柬 ${RUN_ID}"),\"hostName\":$(json_escape "${HOST_NAME}"),\"contactPhone\":\"13900000000\",\"addressDetail\":\"生产验收酒店三楼\",\"scheduleText\":\"17:30 签到\\n18:00 开席\",\"greeting\":\"欢迎参加生产验收宴席\",\"showGiftEntry\":true,\"showDeviceEntry\":false}"

request public_invitation "${BASE_URL}/api/invitations/public/${SHARE_SLUG}"
assert_json public_invitation "data.code === 0 && data.data.invitation.id === Number('${INVITATION_ID}')" "public invitation read failed"
assert_json public_invitation "data.data.shareSlug === '${SHARE_SLUG}' || data.data.shareUrl.includes('${SHARE_SLUG}')" "public invitation share slug missing"

request rsvp_submit -X POST "${BASE_URL}/api/rsvp/submit" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"guestName\":$(json_escape "${RSVP_GUEST}"),\"attendanceStatus\":\"ATTENDING\",\"mealRequired\":1,\"guestCount\":2}"
assert_json rsvp_submit "data.code === 0 && data.data.guestCount === 2" "rsvp submit failed"

request rsvp_stats "${BASE_URL}/api/rsvp/stats?banquetId=${BANQUET_ID}"
assert_json rsvp_stats "data.code === 0 && data.data.totalGuests >= 2" "rsvp stats failed"

request offline_gift -X POST "${BASE_URL}/api/gifts/offline" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"guestName\":$(json_escape "${CASH_GUEST}"),\"amount\":88.00,\"blessing\":\"生产验收线下记礼\"}"
assert_json offline_gift "data.code === 0 && data.data.giftSource === 'CASH'" "offline gift failed"

request gift_summary "${BASE_URL}/api/gifts/summary?banquetId=${BANQUET_ID}"
assert_json gift_summary "data.code === 0 && data.data.totalRecords >= 1" "gift summary failed"

request confirm_bind -X POST "${BASE_URL}/api/confirm-screen/bind" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"bindCode\":$(json_escape "${CONFIRM_BIND_CODE}")}"
request confirm_status "${BASE_URL}/api/confirm-screen/status/${CONFIRM_BIND_CODE}"
assert_json confirm_status "data.code === 0 && data.data.bindStatus === 'BOUND'" "confirm screen status failed"

PAYMENT_STATUS="$(request_status payment_order -X POST "${BASE_URL}/api/gifts/payment-orders" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"entrySource\":\"ONLINE_GIFT\",\"guestName\":$(json_escape "${ONLINE_GUEST}"),\"amount\":1.00,\"blessing\":\"生产验收在线支付订单\",\"payerOpenId\":\"prod-acceptance-${RUN_ID}\"}")"

PAYMENT_RESULT="created"
PAYMENT_ORDER_NO=""
if [[ "$PAYMENT_STATUS" =~ ^2 ]]; then
  assert_json payment_order "data.code === 0 && data.data.order.orderNo" "payment order response missing order"
  PAYMENT_ORDER_NO="$(json_get payment_order "data.data.order.orderNo")"
elif [[ "$READINESS_STATUS" == "BLOCKED" && "$PAYMENT_STATUS" == "400" ]]; then
  PAYMENT_RESULT="blocked-as-expected"
else
  echo "payment order returned unexpected status ${PAYMENT_STATUS}; artifacts: ${WORK_DIR}" >&2
  exit 1
fi

request admin_gifts "${BASE_URL}/api/admin/gifts?banquetId=${BANQUET_ID}" "${AUTH_HEADER[@]}"
assert_json admin_gifts "data.code === 0 && (Array.isArray(data.data) ? data.data : data.data.records).some((item) => item.guestName === '${CASH_GUEST}')" "admin gift list missing offline gift"

request admin_rsvp "${BASE_URL}/api/admin/rsvp?banquetId=${BANQUET_ID}" "${AUTH_HEADER[@]}"
assert_json admin_rsvp "data.code === 0 && (Array.isArray(data.data) ? data.data : data.data.records).some((item) => item.guestName === '${RSVP_GUEST}')" "admin rsvp list missing record"

request broadcast_logs "${BASE_URL}/api/admin/broadcast-logs?banquetId=${BANQUET_ID}" "${AUTH_HEADER[@]}"
assert_json broadcast_logs "data.code === 0" "admin broadcast logs failed"

request operation_logs "${BASE_URL}/api/admin/operation-logs" "${AUTH_HEADER[@]}"
assert_json operation_logs "data.code === 0" "admin operation logs failed"

cat > "${WORK_DIR}/summary.json" <<JSON
{
  "baseUrl": "${BASE_URL}",
  "readinessStatus": "${READINESS_STATUS}",
  "banquetId": ${BANQUET_ID},
  "invitationId": ${INVITATION_ID},
  "shareSlug": "${SHARE_SLUG}",
  "confirmScreenBindCode": "${CONFIRM_BIND_CODE}",
  "paymentResult": "${PAYMENT_RESULT}",
  "paymentOrderNo": "${PAYMENT_ORDER_NO}",
  "artifactsDir": "${WORK_DIR}"
}
JSON

echo "Production API acceptance passed. Artifacts: ${WORK_DIR}"
echo "Banquet ID: ${BANQUET_ID}"
echo "Invitation ID: ${INVITATION_ID}"
echo "Share slug: ${SHARE_SLUG}"
echo "Confirm-screen bind code: ${CONFIRM_BIND_CODE}"
echo "Payment result: ${PAYMENT_RESULT}${PAYMENT_ORDER_NO:+ (${PAYMENT_ORDER_NO})}"
