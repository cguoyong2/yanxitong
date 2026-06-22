#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
RUN_ID="$(date +%Y%m%d%H%M%S)"
WORK_DIR="${TMPDIR:-/tmp}/yanxitong-smoke-${RUN_ID}"
mkdir -p "$WORK_DIR"

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
  node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('${WORK_DIR}/${file}.json','utf8')); console.log(${expr});"
}

assert_json() {
  local file="$1"
  local expr="$2"
  local message="$3"
  node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('${WORK_DIR}/${file}.json','utf8')); if (!(${expr})) { throw new Error('${message}'); }"
}

assert_xlsx_contains() {
  local file="$1"
  local text="$2"
  local message="$3"
  if [[ "$(head -c 2 "${WORK_DIR}/${file}.json")" != "PK" ]]; then
    echo "${message}: xlsx zip header missing" >&2
    exit 1
  fi
  if ! unzip -p "${WORK_DIR}/${file}.json" xl/sharedStrings.xml | grep -q "${text}"; then
    echo "${message}" >&2
    exit 1
  fi
}

url_encode() {
  node -e "process.stdout.write(encodeURIComponent(process.argv[1]))" "$1"
}

mock_signature() {
  node -e "const crypto=require('crypto'); process.stdout.write(crypto.createHmac('sha256', process.env.PAYMENT_MOCK_CALLBACK_SECRET || 'yanxitong-mock-callback-secret').update(process.argv[1]).digest('hex'))" "$1"
}

request health "${BASE_URL}/api/health"
assert_json health "data.code === 0 && data.data.status === 'UP'" "health check failed"

request runtime_features "${BASE_URL}/api/runtime/features"
if ! node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('${WORK_DIR}/runtime_features.json','utf8')); process.exit(data.code === 0 && data.data && data.data.mockPaymentEnabled === true ? 0 : 1);"; then
  echo "mock payment success is disabled. Set PAYMENT_MOCK_SUCCESS_ENABLED=true before running backend smoke tests." >&2
  exit 1
fi

request login -X POST "${BASE_URL}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}"
TOKEN="$(json_get login "data.data.token")"

UNAUTH_STATUS="$(request_status admin_unauth "${BASE_URL}/api/admin/config-items")"
if [[ "$UNAUTH_STATUS" != "401" ]]; then
  echo "expected unauthorized admin request to return 401, got ${UNAUTH_STATUS}" >&2
  exit 1
fi

AUTH_HEADER=(-H "Authorization: Bearer ${TOKEN}")

request admin_config "${BASE_URL}/api/admin/config-items" "${AUTH_HEADER[@]}"
assert_json admin_config "data.code === 0 && Array.isArray(data.data)" "admin config list failed"

THEME_CODE="smoke_theme_${RUN_ID}"
EVENT_TYPE_CODE="SMOKE_${RUN_ID}"
DEVICE_DELIVERY="联调配送_${RUN_ID}"
ONLINE_GUEST="联调支付_${RUN_ID}"
QR_GUEST="联调扫码_${RUN_ID}"
CASH_GUEST="联调现金_${RUN_ID}"
ONLINE_GUEST_Q="$(url_encode "${ONLINE_GUEST}")"
QR_GUEST_Q="$(url_encode "${QR_GUEST}")"

request theme_create -X POST "${BASE_URL}/api/admin/themes" "${AUTH_HEADER[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"themeCode\":\"${THEME_CODE}\",\"name\":\"联调主题\",\"primaryColor\":\"#B91C1C\",\"secondaryColor\":\"#FACC15\",\"iconStyle\":\"celebration\",\"confirmScreenTemplate\":\"default_success\",\"enabled\":1}"

request event_type_create -X POST "${BASE_URL}/api/admin/event-types" "${AUTH_HEADER[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"eventTypeCode\":\"${EVENT_TYPE_CODE}\",\"name\":\"联调宴席\",\"alias\":\"联调\",\"defaultThemeCode\":\"${THEME_CODE}\",\"defaultCopywriting\":\"联调默认文案\",\"sortOrder\":999,\"enabled\":1}"

request copywriting_create -X POST "${BASE_URL}/api/admin/theme-copywriting" "${AUTH_HEADER[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"themeCode\":\"${THEME_CODE}\",\"eventTypeCode\":\"${EVENT_TYPE_CODE}\",\"sceneCode\":\"GIFT_SUCCESS\",\"title\":\"联调到账\",\"content\":\"联调主题文案\",\"speakerText\":\"联调播报文案\",\"enabled\":1}"

request meta_event_types "${BASE_URL}/api/meta/event-types"
assert_json meta_event_types "data.data.some((item) => item.eventTypeCode === '${EVENT_TYPE_CODE}' && item.defaultThemeCode === '${THEME_CODE}')" "public event type meta missing"
request meta_templates "${BASE_URL}/api/meta/invitation-templates"
assert_json meta_templates "data.code === 0 && data.data.length >= 1" "public invitation template meta missing"
assert_json meta_templates "data.data.some((item) => item.templateCode === 'PREMIUM_CEREMONY')" "template presets missing"
assert_json meta_templates "data.data.some((item) => item.templateCode === 'ELEGANT_WEDDING') && data.data.some((item) => item.templateCode === 'MEMORIAL_SIMPLE')" "rich invitation template presets missing"
assert_json meta_templates "data.data.every((item) => item.presentation && item.presentation.fallbackCoverLabel)" "template meta presentation missing"
TEMPLATE_ID="$(json_get meta_templates "data.data[0].id")"

request banquet_create -X POST "${BASE_URL}/api/banquets" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"联调宴席 ${RUN_ID}\",\"eventTypeCode\":\"${EVENT_TYPE_CODE}\",\"banquetTime\":\"2026-10-01T18:00:00\",\"location\":\"联调酒店\",\"templateId\":${TEMPLATE_ID}}"
BANQUET_ID="$(json_get banquet_create "data.data.banquet.id")"
INVITATION_ID="$(json_get banquet_create "data.data.invitation.id")"
SHARE_SLUG="$(json_get banquet_create "data.data.invitation.shareSlug")"

request invitation_basic_update -X PUT "${BASE_URL}/api/invitations/${INVITATION_ID}/basic" \
  -H 'Content-Type: application/json' \
  -d "{\"title\":\"联调请柬 ${RUN_ID}\",\"hostName\":\"联调主办人\",\"contactPhone\":\"13900000000\",\"coverUrl\":\"https://example.com/cover.jpg\",\"addressDetail\":\"联调酒店三楼宴会厅\",\"scheduleText\":\"17:30 签到\\n18:00 开席\",\"greeting\":\"欢迎参加联调宴席\",\"showGiftEntry\":true,\"showDeviceEntry\":false}"

request public_invitation "${BASE_URL}/api/invitations/public/${SHARE_SLUG}"
assert_json public_invitation "data.data.banquet.themeCode === '${THEME_CODE}'" "event type default theme did not affect banquet"
assert_json public_invitation "data.data.template && data.data.template.id === ${TEMPLATE_ID}" "public invitation template missing"
assert_json public_invitation "data.data.templatePresentation && data.data.templatePresentation.defaultScheduleText.includes('签到')" "public invitation template presentation missing"
assert_json public_invitation "data.data.giftSuccessCopywriting.content === '联调主题文案'" "theme copywriting did not affect invitation"
assert_json public_invitation "data.data.invitation.title === '联调请柬 ${RUN_ID}'" "invitation basic title update failed"
assert_json public_invitation "JSON.parse(data.data.invitation.basicFields).hostName === '联调主办人'" "invitation basic hostName update failed"
assert_json public_invitation "JSON.parse(data.data.invitation.basicFields).greeting === '欢迎参加联调宴席'" "invitation basic greeting update failed"
assert_json public_invitation "data.data.basicFields.hostName === '联调主办人' && data.data.shareUrl.includes('${SHARE_SLUG}')" "public invitation normalized fields missing"
assert_json public_invitation "data.data.actionUrls.rsvp.includes('banquetId=${BANQUET_ID}') && data.data.actionUrls.onlineGift.includes('ONLINE_GIFT')" "public invitation action urls missing"
assert_json public_invitation "data.data.basicFields.contactPhone === '13900000000' && data.data.basicFields.addressDetail.includes('宴会厅')" "public invitation contact/address fields missing"
assert_json public_invitation "data.data.basicFields.scheduleText.includes('签到') && data.data.basicFields.showDeviceEntry === '0'" "public invitation schedule/entry flags missing"
request invitation_detail "${BASE_URL}/api/invitations/${INVITATION_ID}"
assert_json invitation_detail "data.data.invitation.id === ${INVITATION_ID} && data.data.basicFields.contactPhone === '13900000000'" "invitation detail backfill fields missing"
assert_json invitation_detail "data.data.shareUrl.includes('${SHARE_SLUG}')" "invitation detail share url missing"

EVENT_TYPE_ID="$(json_get event_type_create "data.data.id")"
DELETE_EVENT_STATUS="$(request_status delete_used_event_type -X DELETE "${BASE_URL}/api/admin/event-types/${EVENT_TYPE_ID}" "${AUTH_HEADER[@]}")"
if [[ "$DELETE_EVENT_STATUS" != "400" ]]; then
  echo "expected used event type delete to return 400, got ${DELETE_EVENT_STATUS}" >&2
  exit 1
fi

request rsvp_submit -X POST "${BASE_URL}/api/rsvp/submit" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"guestName\":\"联调来宾\",\"attendanceStatus\":\"ATTEND\",\"mealRequired\":1,\"guestCount\":2}"
RSVP_ID="$(json_get rsvp_submit "data.data.id")"
assert_json rsvp_submit "data.data.created === true" "first rsvp submit should be marked created"
request rsvp_submit_repeat -X POST "${BASE_URL}/api/rsvp/submit" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"guestName\":\"联调来宾\",\"attendanceStatus\":\"ATTENDING\",\"mealRequired\":1,\"guestCount\":3}"
assert_json rsvp_submit_repeat "data.data.id === ${RSVP_ID} && data.data.guestCount === 3" "rsvp repeat submit should update existing record"
assert_json rsvp_submit_repeat "data.data.created === false" "repeat rsvp submit should be marked updated"
INVALID_RSVP_STATUS="$(request_status rsvp_invalid -X POST "${BASE_URL}/api/rsvp/submit" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"guestName\":\"联调非法\",\"attendanceStatus\":\"UNKNOWN\",\"guestCount\":1}")"
if [[ "$INVALID_RSVP_STATUS" != "400" ]]; then
  echo "expected invalid rsvp status to return 400, got ${INVALID_RSVP_STATUS}" >&2
  exit 1
fi
request rsvp_stats "${BASE_URL}/api/rsvp/stats?banquetId=${BANQUET_ID}"
assert_json rsvp_stats "data.data.attendingRecords === 1 && data.data.totalGuests === 3" "rsvp stats failed"
assert_json rsvp_stats "data.data.pendingRecords === 0 && data.data.declinedRecords === 0" "rsvp status distribution failed"

request plan_entitlements_default "${BASE_URL}/api/plans/banquets/${BANQUET_ID}/entitlements"
assert_json plan_entitlements_default "data.data.freeDefault === true && data.data.currentPlan.planCode === 'BASIC'" "default plan entitlement failed"
request device_right_before_plan "${BASE_URL}/api/plans/banquets/${BANQUET_ID}/rights/check?rightCode=DEVICE_RENTAL"
assert_json device_right_before_plan "data.data.allowed === false" "device right should be unavailable before plan purchase"

request device_config -X POST "${BASE_URL}/api/admin/device-configs" "${AUTH_HEADER[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deviceType\":\"CONFIRM_SCREEN\",\"name\":\"联调确认屏\",\"price\":321.00,\"priceUnit\":\"场\",\"deliveryMethod\":\"${DEVICE_DELIVERY}\",\"enabled\":1}"

BLOCKED_DEVICE_STATUS="$(request_status device_order_blocked -X POST "${BASE_URL}/api/devices/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"deviceType\":\"CONFIRM_SCREEN\",\"rentStartAt\":\"2026-10-01T17:00:00\",\"rentEndAt\":\"2026-10-01T22:00:00\",\"deliveryMethod\":\"${DEVICE_DELIVERY}\"}")"
if [[ "$BLOCKED_DEVICE_STATUS" != "400" ]]; then
  echo "expected device order without right to return 400, got ${BLOCKED_DEVICE_STATUS}" >&2
  exit 1
fi

request plans "${BASE_URL}/api/plans"
PRO_PLAN_ID="$(json_get plans "data.data.find((item) => item.planCode === 'PRO').id")"
request plan_order -X POST "${BASE_URL}/api/plans/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"planId\":${PRO_PLAN_ID}}"
PLAN_ORDER_NO="$(json_get plan_order "data.data.orderNo")"
assert_json plan_order "data.data.payStatus === 'UNPAID'" "paid plan order should start unpaid"
request plan_order_repeat -X POST "${BASE_URL}/api/plans/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"planId\":${PRO_PLAN_ID}}"
assert_json plan_order_repeat "data.data.orderNo === '${PLAN_ORDER_NO}'" "repeat plan order should return existing order"
request plan_order_paid -X POST "${BASE_URL}/api/plans/orders/${PLAN_ORDER_NO}/mock-success"
assert_json plan_order_paid "data.data.payStatus === 'PAID'" "mock plan payment failed"
request plan_entitlements_paid "${BASE_URL}/api/plans/banquets/${BANQUET_ID}/entitlements"
assert_json plan_entitlements_paid "data.data.paidPlanActive === true && data.data.rightValues.DEVICE_RENTAL === 'INCLUDED'" "paid plan entitlement missing device right"

request device_order -X POST "${BASE_URL}/api/devices/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"deviceType\":\"CONFIRM_SCREEN\",\"rentStartAt\":\"2026-10-01T17:00:00\",\"rentEndAt\":\"2026-10-01T22:00:00\",\"deliveryMethod\":\"${DEVICE_DELIVERY}\"}"
assert_json device_order "Number(data.data.price) === 321" "device config price did not affect order"
DEVICE_ORDER_NO="$(json_get device_order "data.data.orderNo")"
request device_order_repeat -X POST "${BASE_URL}/api/devices/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"deviceType\":\"CONFIRM_SCREEN\",\"rentStartAt\":\"2026-10-01T17:00:00\",\"rentEndAt\":\"2026-10-01T22:00:00\",\"deliveryMethod\":\"${DEVICE_DELIVERY}\"}"
assert_json device_order_repeat "data.data.orderNo === '${DEVICE_ORDER_NO}'" "repeat device order should return existing order"
request device_order_paid -X POST "${BASE_URL}/api/devices/orders/${DEVICE_ORDER_NO}/mock-success"
assert_json device_order_paid "data.data.payStatus === 'PAID' && data.data.orderStatus === 'CONFIRMED'" "mock device payment failed"
request admin_device_delivering -X POST "${BASE_URL}/api/admin/orders/devices/${DEVICE_ORDER_NO}/status" "${AUTH_HEADER[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"orderStatus\":\"DELIVERING\"}"
assert_json admin_device_delivering "data.data.orderStatus === 'DELIVERING'" "device order delivering status failed"
request admin_device_delivered -X POST "${BASE_URL}/api/admin/orders/devices/${DEVICE_ORDER_NO}/status" "${AUTH_HEADER[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"orderStatus\":\"DELIVERED\"}"
assert_json admin_device_delivered "data.data.orderStatus === 'DELIVERED'" "device order delivered status failed"

request confirm_bind -X POST "${BASE_URL}/api/confirm-screen/bind" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"bindCode\":\"CS-${RUN_ID}\"}"
request confirm_status "${BASE_URL}/api/confirm-screen/status/CS-${RUN_ID}"
assert_json confirm_status "data.data.bindStatus === 'BOUND' && data.data.online === false && data.data.onlineSessions === 0" "confirm screen status failed"

request gift_order -X POST "${BASE_URL}/api/gifts/payment-orders" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"entrySource\":\"ONLINE_GIFT\",\"guestName\":\"${ONLINE_GUEST}\",\"amount\":188.88,\"blessing\":\"祝福\",\"payerOpenId\":\"openid-${RUN_ID}\"}"
ORDER_NO="$(json_get gift_order "data.data.order.orderNo")"
request gift_success -X POST "${BASE_URL}/api/gifts/payment-orders/${ORDER_NO}/mock-success"
assert_json gift_success "data.data.blessing === '祝福'" "online gift blessing was not persisted"
request confirm_latest_event "${BASE_URL}/api/confirm-screen/banquets/${BANQUET_ID}/latest-event"
assert_json confirm_latest_event "data.data.guestName === '${ONLINE_GUEST}' && Number(data.data.amount) === 188.88" "confirm screen latest event failed"
request gift_order_qr -X POST "${BASE_URL}/api/gifts/payment-orders" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"entrySource\":\"ONSITE_QR\",\"guestName\":\"${QR_GUEST}\",\"amount\":66.66,\"blessing\":\"现场祝福\",\"payerOpenId\":\"openid-qr-${RUN_ID}\"}"
QR_ORDER_NO="$(json_get gift_order_qr "data.data.order.orderNo")"
request gift_success_qr -X POST "${BASE_URL}/api/gifts/payment-orders/${QR_ORDER_NO}/mock-success"
request gift_offline -X POST "${BASE_URL}/api/gifts/offline" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"guestName\":\"${CASH_GUEST}\",\"amount\":88.00,\"blessing\":\"现金备注\"}"
request gift_summary "${BASE_URL}/api/gifts/summary?banquetId=${BANQUET_ID}"
assert_json gift_summary "data.data.totalRecords === 3 && Number(data.data.sourceAmounts.ONLINE_GIFT) === 188.88 && Number(data.data.sourceAmounts.ONSITE_QR) === 66.66 && Number(data.data.sourceAmounts.CASH) === 88" "gift summary failed"
request gift_filter "${BASE_URL}/api/gifts?banquetId=${BANQUET_ID}&source=ONSITE_QR&keyword=${QR_GUEST_Q}"
assert_json gift_filter "data.data.length === 1 && data.data[0].guestName === '${QR_GUEST}'" "gift filter failed"
request favor_contacts "${BASE_URL}/api/favor/contacts"
assert_json favor_contacts "data.data.length >= 1" "favor ledger was not written"
request favor_search "${BASE_URL}/api/favor/contacts?keyword=${ONLINE_GUEST_Q}"
assert_json favor_search "data.data.length === 1 && Number(data.data[0].receivedAmount) === 188.88" "favor contact search failed"
request favor_compare "${BASE_URL}/api/favor/compare?contactName=${ONLINE_GUEST_Q}"
assert_json favor_compare "Number(data.data.receivedAmount) === 188.88 && data.data.entries[0].note === '祝福'" "favor compare failed"
INVALID_FAVOR_STATUS="$(request_status favor_invalid -X POST "${BASE_URL}/api/favor/manual" \
  -H 'Content-Type: application/json' \
  -d "{\"contactName\":\"非法方向_${RUN_ID}\",\"direction\":\"BAD\",\"amount\":1}")"
if [[ "$INVALID_FAVOR_STATUS" != "400" ]]; then
  echo "expected invalid favor direction to return 400, got ${INVALID_FAVOR_STATUS}" >&2
  exit 1
fi
request broadcast_logs "${BASE_URL}/api/admin/broadcast-logs?banquetId=${BANQUET_ID}" "${AUTH_HEADER[@]}"
assert_json broadcast_logs "(Array.isArray(data.data) ? data.data : data.data.records).length >= 6" "broadcast logs missing"
request cloud_speaker_logs "${BASE_URL}/api/admin/broadcast-logs?banquetId=${BANQUET_ID}&deviceType=CLOUD_SPEAKER" "${AUTH_HEADER[@]}"
assert_json cloud_speaker_logs "(Array.isArray(data.data) ? data.data : data.data.records).length >= 3 && (Array.isArray(data.data) ? data.data : data.data.records).every((item) => item.status === 'SIMULATED')" "cloud speaker simulated logs missing"
request confirm_screen_logs "${BASE_URL}/api/admin/broadcast-logs?banquetId=${BANQUET_ID}&deviceType=CONFIRM_SCREEN&status=OFFLINE" "${AUTH_HEADER[@]}"
assert_json confirm_screen_logs "(Array.isArray(data.data) ? data.data : data.data.records).length >= 3" "confirm screen offline logs missing"

request operation_logs "${BASE_URL}/api/admin/operation-logs" "${AUTH_HEADER[@]}"
assert_json operation_logs "(Array.isArray(data.data) ? data.data : data.data.records).length >= 8" "operation logs missing"

request admin_gifts "${BASE_URL}/api/admin/gifts?banquetId=${BANQUET_ID}" "${AUTH_HEADER[@]}"
assert_json admin_gifts "(Array.isArray(data.data) ? data.data : data.data.records).length >= 3 && (Array.isArray(data.data) ? data.data : data.data.records).some((item) => item.guestName === '${CASH_GUEST}')" "admin gift list missing records"
request admin_rsvp "${BASE_URL}/api/admin/rsvp?banquetId=${BANQUET_ID}" "${AUTH_HEADER[@]}"
assert_json admin_rsvp "(Array.isArray(data.data) ? data.data : data.data.records).length >= 1 && (Array.isArray(data.data) ? data.data : data.data.records)[0].guestName === '联调来宾'" "admin rsvp list missing records"
request admin_rsvp_stats "${BASE_URL}/api/admin/rsvp/stats?banquetId=${BANQUET_ID}" "${AUTH_HEADER[@]}"
assert_json admin_rsvp_stats "data.data.totalGuests === 3" "admin rsvp stats failed"
request admin_favor_contacts "${BASE_URL}/api/admin/favor/contacts?keyword=${ONLINE_GUEST_Q}" "${AUTH_HEADER[@]}"
assert_json admin_favor_contacts "data.data.length === 1 && Number(data.data[0].receivedAmount) === 188.88" "admin favor contacts failed"
FAVOR_CONTACT_ID="$(json_get admin_favor_contacts "data.data[0].contactId")"
request admin_favor_detail "${BASE_URL}/api/admin/favor/contacts/${FAVOR_CONTACT_ID}" "${AUTH_HEADER[@]}"
assert_json admin_favor_detail "data.data.entries.length >= 1 && Number(data.data.receivedAmount) === 188.88" "admin favor detail failed"
request admin_favor_manual -X POST "${BASE_URL}/api/admin/favor/manual" "${AUTH_HEADER[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"contactName\":\"后台补录_${RUN_ID}\",\"direction\":\"GIVEN\",\"amount\":123.00,\"note\":\"后台人情补录\"}"
assert_json admin_favor_manual "data.data.sourceType === 'MANUAL' && data.data.direction === 'GIVEN'" "admin favor manual failed"
request admin_offline_gift -X POST "${BASE_URL}/api/admin/gifts/offline" "${AUTH_HEADER[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"banquetId\":${BANQUET_ID},\"guestName\":\"后台现金_${RUN_ID}\",\"amount\":55.00,\"blessing\":\"后台现金记礼\"}"
assert_json admin_offline_gift "data.data.giftSource === 'CASH' && data.data.guestName === '后台现金_${RUN_ID}'" "admin offline gift failed"

request payment_orders "${BASE_URL}/api/admin/payments/orders" "${AUTH_HEADER[@]}"
assert_json payment_orders "(Array.isArray(data.data) ? data.data : data.data.records).some((item) => item.orderNo === '${ORDER_NO}' && item.payStatus === 'PAID')" "admin payment order list missing paid gift order"

request payment_providers "${BASE_URL}/api/admin/payments/providers" "${AUTH_HEADER[@]}"
assert_json payment_providers "data.data.some((item) => item.provider === 'MOCK' && item.enabled === true && item.callbackSecretConfigured === true)" "admin payment provider status missing mock config"

MISSING_ORDER_NO="GP_MISSING_${RUN_ID}"
MISSING_CALLBACK_JSON="$(node -e "process.stdout.write(JSON.stringify({orderNo:'${MISSING_ORDER_NO}', providerTradeNo:'MOCK-MISSING-${RUN_ID}', paidAmount:12.34, success:true}))")"
MISSING_CALLBACK_RAW="$(node -e "process.stdout.write(JSON.stringify(process.argv[1]))" "${MISSING_CALLBACK_JSON}")"
MISSING_CALLBACK_SIGNATURE="$(mock_signature "${MISSING_CALLBACK_JSON}")"
request payment_missing_callback -X POST "${BASE_URL}/api/payments/callbacks" \
  -H 'Content-Type: application/json' \
  -d "{\"provider\":\"MOCK\",\"rawBody\":${MISSING_CALLBACK_RAW},\"signature\":\"${MISSING_CALLBACK_SIGNATURE}\"}"
MISSING_CALLBACK_ID="$(json_get payment_missing_callback "data.data.id")"
assert_json payment_missing_callback "data.data.verifyStatus === 'VERIFIED' && data.data.processStatus === 'FAILED'" "missing order callback should be recorded as failed"

BAD_SIGNATURE_CALLBACK_JSON="$(node -e "process.stdout.write(JSON.stringify({orderNo:'${ORDER_NO}', providerTradeNo:'MOCK-BAD-SIGN-${RUN_ID}', paidAmount:188.88, success:true}))")"
BAD_SIGNATURE_CALLBACK_RAW="$(node -e "process.stdout.write(JSON.stringify(process.argv[1]))" "${BAD_SIGNATURE_CALLBACK_JSON}")"
request payment_bad_signature_callback -X POST "${BASE_URL}/api/payments/callbacks" \
  -H 'Content-Type: application/json' \
  -d "{\"provider\":\"MOCK\",\"rawBody\":${BAD_SIGNATURE_CALLBACK_RAW},\"signature\":\"bad-signature\"}"
assert_json payment_bad_signature_callback "data.data.verifyStatus === 'FAILED' && data.data.processStatus === 'FAILED' && data.data.errorMessage.includes('signature')" "bad callback signature should be recorded as failed"

request payment_failed_callbacks "${BASE_URL}/api/admin/payments/callbacks?processStatus=FAILED" "${AUTH_HEADER[@]}"
assert_json payment_failed_callbacks "(Array.isArray(data.data) ? data.data : data.data.records).some((item) => item.id === ${MISSING_CALLBACK_ID})" "admin failed callback list missing callback"

request payment_callback_resolve -X POST "${BASE_URL}/api/admin/payments/callbacks/${MISSING_CALLBACK_ID}/resolve" "${AUTH_HEADER[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"processStatus\":\"HANDLED\",\"handleRemark\":\"联调已人工核对\"}"
assert_json payment_callback_resolve "data.data.processStatus === 'HANDLED' && data.data.handleRemark === '联调已人工核对'" "payment callback resolve failed"

request export_gifts "${BASE_URL}/api/admin/exports/banquets/${BANQUET_ID}/gifts.csv" "${AUTH_HEADER[@]}"
if ! grep -q "来宾姓名" "${WORK_DIR}/export_gifts.json" || ! grep -q "${ONLINE_GUEST}" "${WORK_DIR}/export_gifts.json"; then
  echo "gift export csv missing expected content" >&2
  exit 1
fi
request export_gifts_xlsx "${BASE_URL}/api/admin/exports/banquets/${BANQUET_ID}/gifts.xlsx" "${AUTH_HEADER[@]}"
assert_xlsx_contains export_gifts_xlsx "${ONLINE_GUEST}" "gift export xlsx missing expected content"

request export_rsvp "${BASE_URL}/api/admin/exports/banquets/${BANQUET_ID}/rsvp.csv" "${AUTH_HEADER[@]}"
if ! grep -q "出席状态" "${WORK_DIR}/export_rsvp.json" || ! grep -q "联调来宾" "${WORK_DIR}/export_rsvp.json"; then
  echo "rsvp export csv missing expected content" >&2
  exit 1
fi
request export_rsvp_xlsx "${BASE_URL}/api/admin/exports/banquets/${BANQUET_ID}/rsvp.xlsx" "${AUTH_HEADER[@]}"
assert_xlsx_contains export_rsvp_xlsx "联调来宾" "rsvp export xlsx missing expected content"

request export_favor "${BASE_URL}/api/admin/exports/banquets/${BANQUET_ID}/favor.csv" "${AUTH_HEADER[@]}"
if ! grep -q "联系人" "${WORK_DIR}/export_favor.json" || ! grep -q "${ONLINE_GUEST}" "${WORK_DIR}/export_favor.json"; then
  echo "favor export csv missing expected content" >&2
  exit 1
fi
request export_favor_xlsx "${BASE_URL}/api/admin/exports/banquets/${BANQUET_ID}/favor.xlsx" "${AUTH_HEADER[@]}"
assert_xlsx_contains export_favor_xlsx "${ONLINE_GUEST}" "favor export xlsx missing expected content"

echo "Smoke test passed. Artifacts: ${WORK_DIR}"
