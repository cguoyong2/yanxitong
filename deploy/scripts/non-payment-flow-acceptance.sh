#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
BASE_URL="${BASE_URL%/}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)}"
WORK_DIR="${ARTIFACTS_DIR:-.artifacts/non-payment-flow-api/${RUN_ID}}"
mkdir -p "$WORK_DIR"

log() {
  printf '[non-payment-flow] %s\n' "$*"
}

request() {
  local name="$1"
  shift
  curl -fsS "$@" > "${WORK_DIR}/${name}.json"
}

json_get() {
  local file="$1"
  local expr="$2"
  node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); const value=(${expr}); if (value === undefined || value === null) process.exit(2); console.log(typeof value === 'object' ? JSON.stringify(value) : value);" "${WORK_DIR}/${file}.json"
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

log "checking health at ${BASE_URL}"
request health "${BASE_URL}/api/health"
assert_json health "data.code === 0 && data.data.status === 'UP'" "health check failed"

log "logging in as ${ADMIN_USERNAME}"
request login -X POST "${BASE_URL}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":$(json_escape "${ADMIN_USERNAME}"),\"password\":$(json_escape "${ADMIN_PASSWORD}")}"
TOKEN="$(json_get login "data.data.token")"
AUTH_HEADER=(-H "Authorization: Bearer ${TOKEN}")

request meta_event_types "${BASE_URL}/api/meta/event-types"
assert_json meta_event_types "data.code === 0 && data.data.length > 0" "event type meta missing"
EVENT_TYPE_CODE="$(json_get meta_event_types "data.data.find((item) => item.enabled !== 0)?.eventTypeCode || data.data[0].eventTypeCode")"

request meta_templates "${BASE_URL}/api/meta/invitation-templates"
assert_json meta_templates "data.code === 0 && data.data.length > 0" "invitation template meta missing"
TEMPLATE_ID="$(json_get meta_templates "data.data[0].id")"

BANQUET_NAME="非支付验收宴席 ${RUN_ID}"
HOST_NAME="非支付验收主办人 ${RUN_ID}"
RSVP_GUEST="非支付验收到宾 ${RUN_ID}"
CASH_GUEST="非支付线下记礼 ${RUN_ID}"
CASH_GUEST_Q="$(url_encode "${CASH_GUEST}")"

log "creating banquet"
request banquet_create -X POST "${BASE_URL}/api/banquets" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":$(json_escape "${BANQUET_NAME}"),\"eventTypeCode\":$(json_escape "${EVENT_TYPE_CODE}"),\"banquetTime\":\"2026-10-01T18:00:00\",\"location\":\"非支付验收酒店\",\"templateId\":${TEMPLATE_ID}}"
BANQUET_ID="$(json_get banquet_create "data.data.banquet.id")"
INVITATION_ID="$(json_get banquet_create "data.data.invitation.id")"
SHARE_SLUG="$(json_get banquet_create "data.data.invitation.shareSlug")"

request banquet_publish -X POST "${BASE_URL}/api/banquets/${BANQUET_ID}/publish"
assert_json banquet_publish "data.code === 0 && data.data.banquet.status === 'PUBLISHED'" "banquet publish failed"

log "updating and reading public invitation"
request invitation_update -X PUT "${BASE_URL}/api/invitations/${INVITATION_ID}/basic" \
  -H 'Content-Type: application/json' \
  -d "{\"title\":$(json_escape "非支付验收请柬 ${RUN_ID}"),\"hostName\":$(json_escape "${HOST_NAME}"),\"contactPhone\":\"13900000000\",\"addressDetail\":\"非支付验收酒店三楼\",\"scheduleText\":\"17:30 签到\\n18:00 开席\",\"greeting\":\"欢迎参加非支付验收宴席\",\"showGiftEntry\":true,\"showDeviceEntry\":true}"
assert_json invitation_update "data.code === 0 && data.data.id === Number('${INVITATION_ID}')" "invitation update failed"

request public_invitation "${BASE_URL}/api/invitations/public/${SHARE_SLUG}"
assert_json public_invitation "data.code === 0 && data.data.banquet.id === Number('${BANQUET_ID}')" "public invitation banquet mismatch"
assert_json public_invitation "data.data.invitation.title === '非支付验收请柬 ${RUN_ID}'" "public invitation title missing"
assert_json public_invitation "data.data.basicFields.hostName === '${HOST_NAME}'" "public invitation basic fields missing"
assert_json public_invitation "data.data.actionUrls.rsvp.includes('banquetId=${BANQUET_ID}')" "public invitation rsvp action missing"

log "submitting RSVP and checking stats"
request rsvp_submit -X POST "${BASE_URL}/api/rsvp/submit" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"invitationId\":${INVITATION_ID},\"guestName\":$(json_escape "${RSVP_GUEST}"),\"phone\":\"13900000001\",\"attendanceStatus\":\"ATTENDING\",\"mealRequired\":1,\"accommodationRequired\":0,\"guestCount\":2,\"message\":\"准时参加\"}"
RSVP_ID="$(json_get rsvp_submit "data.data.id")"
assert_json rsvp_submit "data.code === 0 && data.data.guestCount === 2 && data.data.attendanceStatus === 'ATTENDING'" "rsvp submit failed"

request rsvp_stats "${BASE_URL}/api/rsvp/stats?banquetId=${BANQUET_ID}"
assert_json rsvp_stats "data.code === 0 && data.data.attendingRecords >= 1 && data.data.totalGuests >= 2" "rsvp stats did not include submitted guest"

log "recording offline gift and checking gift/favor data"
request offline_gift -X POST "${BASE_URL}/api/gifts/offline" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"guestName\":$(json_escape "${CASH_GUEST}"),\"amount\":188.00,\"blessing\":\"非支付验收线下记礼\"}"
assert_json offline_gift "data.code === 0 && data.data.giftSource === 'CASH' && Number(data.data.amount) === 188" "offline gift failed"

request gifts_list "${BASE_URL}/api/gifts?banquetId=${BANQUET_ID}&source=CASH&keyword=${CASH_GUEST_Q}"
assert_json gifts_list "data.code === 0 && data.data.length === 1 && data.data[0].guestName === '${CASH_GUEST}'" "gift list missing offline record"

request favor_contacts "${BASE_URL}/api/favor/contacts?banquetId=${BANQUET_ID}&keyword=${CASH_GUEST_Q}"
assert_json favor_contacts "data.code === 0 && data.data.length === 1 && Number(data.data[0].receivedAmount) === 188" "favor contacts missing offline gift"
FAVOR_CONTACT_ID="$(json_get favor_contacts "data.data[0].contactId")"

request favor_detail "${BASE_URL}/api/favor/contacts/${FAVOR_CONTACT_ID}"
assert_json favor_detail "data.code === 0 && Number(data.data.receivedAmount) === 188 && data.data.entries.some((item) => item.sourceType === 'CASH' && item.giftRecordId)" "favor detail missing gift entry"

request favor_compare "${BASE_URL}/api/favor/compare?contactName=${CASH_GUEST_Q}"
assert_json favor_compare "data.code === 0 && Number(data.data.receivedAmount) === 188 && data.data.entries.some((item) => item.note === '非支付验收线下记礼')" "favor compare missing gift note"

log "checking admin protected read paths"
request admin_rsvp "${BASE_URL}/api/admin/rsvp?banquetId=${BANQUET_ID}" "${AUTH_HEADER[@]}"
assert_json admin_rsvp "data.code === 0 && (Array.isArray(data.data) ? data.data : data.data.records).some((item) => item.id === Number('${RSVP_ID}'))" "admin rsvp missing submitted record"

request admin_gifts "${BASE_URL}/api/admin/gifts?banquetId=${BANQUET_ID}" "${AUTH_HEADER[@]}"
assert_json admin_gifts "data.code === 0 && (Array.isArray(data.data) ? data.data : data.data.records).some((item) => item.guestName === '${CASH_GUEST}')" "admin gifts missing offline record"

cat > "${WORK_DIR}/summary.json" <<JSON
{
  "baseUrl": "${BASE_URL}",
  "banquetId": ${BANQUET_ID},
  "invitationId": ${INVITATION_ID},
  "shareSlug": "${SHARE_SLUG}",
  "rsvpId": ${RSVP_ID},
  "favorContactId": ${FAVOR_CONTACT_ID},
  "artifactsDir": "${WORK_DIR}"
}
JSON

log "passed. Artifacts: ${WORK_DIR}"
log "banquetId=${BANQUET_ID}, invitationId=${INVITATION_ID}, shareSlug=${SHARE_SLUG}"
