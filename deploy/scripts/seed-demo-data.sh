#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
DEMO_KEY="${DEMO_KEY:-standard}"
RUN_ID="$(date +%Y%m%d%H%M%S)"
WORK_DIR="${TMPDIR:-/tmp}/yanxitong-demo-${DEMO_KEY}-${RUN_ID}"
mkdir -p "$WORK_DIR"

request() {
  local name="$1"
  shift
  local auth_args=()
  local arg
  if [[ -n "${MINIAPP_TOKEN:-}" ]]; then
    for arg in "$@"; do
      if [[ "${arg}" == Authorization:\ Bearer\ * ]]; then
        curl -fsS "$@" > "${WORK_DIR}/${name}.json"
        return
      fi
    done
    auth_args=(-H "Authorization: Bearer ${MINIAPP_TOKEN}")
  fi
  curl -fsS "$@" "${auth_args[@]}" > "${WORK_DIR}/${name}.json"
}

json_get() {
  local file="$1"
  local expr="$2"
  node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('${WORK_DIR}/${file}.json','utf8')); const value=(${expr}); if (value === undefined || value === null) process.exit(2); if (typeof value === 'object') console.log(JSON.stringify(value)); else console.log(value);"
}

json_has() {
  local file="$1"
  local expr="$2"
  node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('${WORK_DIR}/${file}.json','utf8')); process.exit((${expr}) ? 0 : 1);"
}

url_encode() {
  node -e "process.stdout.write(encodeURIComponent(process.argv[1]))" "$1"
}

api_get() {
  local name="$1"
  local path="$2"
  shift 2
  request "$name" "${BASE_URL}${path}" "$@"
}

api_post() {
  local name="$1"
  local path="$2"
  local body="$3"
  shift 3
  request "$name" -X POST "${BASE_URL}${path}" "$@" -H 'Content-Type: application/json' -d "$body"
}

api_put() {
  local name="$1"
  local path="$2"
  local body="$3"
  shift 3
  request "$name" -X PUT "${BASE_URL}${path}" "$@" -H 'Content-Type: application/json' -d "$body"
}

payload() {
  node -e "const value = JSON.parse(process.argv[1]); process.stdout.write(JSON.stringify(value));" "$1"
}

request health "${BASE_URL}/api/health"
if ! json_has health "data.code === 0 && data.data.status === 'UP'"; then
  echo "Backend health check failed. Start the server before seeding demo data." >&2
  exit 1
fi

api_get runtime_features /api/runtime/features
if ! json_has runtime_features "data.code === 0 && data.data.mockPaymentEnabled === true"; then
  echo "mock payment success is disabled. Set PAYMENT_MOCK_SUCCESS_ENABLED=true before seeding demo data." >&2
  exit 1
fi

api_post login /api/auth/login "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}"
TOKEN="$(json_get login "data.data.token")"
AUTH_HEADER=(-H "Authorization: Bearer ${TOKEN}")

THEME_CODE="demo_theme_${DEMO_KEY}"
EVENT_TYPE_CODE="DEMO_${DEMO_KEY}"
DEVICE_DELIVERY="演示配送-${DEMO_KEY}"
BIND_CODE="DEMO-CS-${DEMO_KEY}-${RUN_ID}"

api_get admin_themes /api/admin/themes "${AUTH_HEADER[@]}"
if json_has admin_themes "data.data.some((item) => item.themeCode === '${THEME_CODE}')"; then
  THEME_ID="$(json_get admin_themes "data.data.find((item) => item.themeCode === '${THEME_CODE}').id")"
else
  api_post theme_create /api/admin/themes "{\"themeCode\":\"${THEME_CODE}\",\"name\":\"演示主题-${DEMO_KEY}\",\"primaryColor\":\"#B91C1C\",\"secondaryColor\":\"#FACC15\",\"iconStyle\":\"celebration\",\"confirmScreenTemplate\":\"default_success\",\"enabled\":1}" "${AUTH_HEADER[@]}"
  THEME_ID="$(json_get theme_create "data.data.id")"
fi

api_get admin_event_types /api/admin/event-types "${AUTH_HEADER[@]}"
if json_has admin_event_types "data.data.some((item) => item.eventTypeCode === '${EVENT_TYPE_CODE}')"; then
  EVENT_TYPE_ID="$(json_get admin_event_types "data.data.find((item) => item.eventTypeCode === '${EVENT_TYPE_CODE}').id")"
else
  api_post event_type_create /api/admin/event-types "{\"eventTypeCode\":\"${EVENT_TYPE_CODE}\",\"name\":\"演示宴席-${DEMO_KEY}\",\"alias\":\"演示\",\"defaultThemeCode\":\"${THEME_CODE}\",\"defaultCopywriting\":\"欢迎参加演示宴席\",\"sortOrder\":900,\"enabled\":1}" "${AUTH_HEADER[@]}"
  EVENT_TYPE_ID="$(json_get event_type_create "data.data.id")"
fi

api_get admin_copywriting /api/admin/theme-copywriting "${AUTH_HEADER[@]}"
if ! json_has admin_copywriting "data.data.some((item) => item.themeCode === '${THEME_CODE}' && item.eventTypeCode === '${EVENT_TYPE_CODE}' && item.sceneCode === 'GIFT_SUCCESS')"; then
  api_post copywriting_create /api/admin/theme-copywriting "{\"themeCode\":\"${THEME_CODE}\",\"eventTypeCode\":\"${EVENT_TYPE_CODE}\",\"sceneCode\":\"GIFT_SUCCESS\",\"title\":\"演示到账\",\"content\":\"感谢来宾的美好祝福\",\"speakerText\":\"收到演示礼金，祝福新人喜乐长久\",\"enabled\":1}" "${AUTH_HEADER[@]}"
fi

api_get admin_device_configs /api/admin/device-configs "${AUTH_HEADER[@]}"
if ! json_has admin_device_configs "data.data.some((item) => item.deviceType === 'CONFIRM_SCREEN' && item.deliveryMethod === '${DEVICE_DELIVERY}')"; then
  api_post device_config_confirm /api/admin/device-configs "{\"deviceType\":\"CONFIRM_SCREEN\",\"name\":\"演示确认屏-${DEMO_KEY}\",\"price\":299.00,\"priceUnit\":\"场\",\"deliveryMethod\":\"${DEVICE_DELIVERY}\",\"enabled\":1}" "${AUTH_HEADER[@]}"
fi
if ! json_has admin_device_configs "data.data.some((item) => item.deviceType === 'CLOUD_SPEAKER' && item.deliveryMethod === '${DEVICE_DELIVERY}')"; then
  api_post device_config_speaker /api/admin/device-configs "{\"deviceType\":\"CLOUD_SPEAKER\",\"name\":\"演示云喇叭-${DEMO_KEY}\",\"price\":99.00,\"priceUnit\":\"场\",\"deliveryMethod\":\"${DEVICE_DELIVERY}\",\"enabled\":1}" "${AUTH_HEADER[@]}"
fi

api_get meta_templates /api/meta/invitation-templates
TEMPLATE_ID="$(json_get meta_templates "data.data[0].id")"

api_post banquet_create /api/banquets "{\"name\":\"演示宴席 ${RUN_ID}\",\"eventTypeCode\":\"${EVENT_TYPE_CODE}\",\"banquetTime\":\"2026-10-01T18:00:00\",\"location\":\"演示酒店 宴会厅A\",\"templateId\":${TEMPLATE_ID}}"
BANQUET_ID="$(json_get banquet_create "data.data.banquet.id")"
INVITATION_ID="$(json_get banquet_create "data.data.invitation.id")"
SHARE_SLUG="$(json_get banquet_create "data.data.invitation.shareSlug")"

api_put invitation_basic_update "/api/invitations/${INVITATION_ID}/basic" "{\"title\":\"演示请柬 ${RUN_ID}\",\"hostName\":\"陈先生 & 林女士\",\"contactPhone\":\"13800008888\",\"coverUrl\":\"https://example.com/demo-cover.jpg\",\"addressDetail\":\"三楼牡丹厅，地铁A口步行5分钟\",\"scheduleText\":\"17:30 来宾签到\\n18:00 仪式开始\\n18:30 宴席开席\",\"greeting\":\"诚邀您参加我们的宴席，共同见证美好时刻。\",\"showGiftEntry\":true,\"showDeviceEntry\":true}"
api_post banquet_publish "/api/banquets/${BANQUET_ID}/publish" "{}"

api_post rsvp_submit /api/rsvp/submit "{\"banquetId\":${BANQUET_ID},\"guestName\":\"演示来宾-张三\",\"phone\":\"13800000001\",\"attendanceStatus\":\"ATTENDING\",\"mealRequired\":1,\"accommodationRequired\":0,\"guestCount\":2}"
api_post rsvp_decline /api/rsvp/submit "{\"banquetId\":${BANQUET_ID},\"guestName\":\"演示来宾-李四\",\"phone\":\"13800000002\",\"attendanceStatus\":\"DECLINED\",\"mealRequired\":0,\"accommodationRequired\":0,\"guestCount\":1}"

api_get plans /api/plans
PRO_PLAN_ID="$(json_get plans "data.data.find((item) => item.planCode === 'PRO').id")"
api_post plan_order /api/plans/orders "{\"banquetId\":${BANQUET_ID},\"planId\":${PRO_PLAN_ID}}"
PLAN_ORDER_NO="$(json_get plan_order "data.data.orderNo")"
api_post plan_order_paid "/api/plans/orders/${PLAN_ORDER_NO}/mock-success" "{}"

api_post device_order_confirm /api/devices/orders "{\"banquetId\":${BANQUET_ID},\"deviceType\":\"CONFIRM_SCREEN\",\"rentStartAt\":\"2026-10-01T17:00:00\",\"rentEndAt\":\"2026-10-01T22:00:00\",\"deliveryMethod\":\"${DEVICE_DELIVERY}\"}"
CONFIRM_DEVICE_ORDER_NO="$(json_get device_order_confirm "data.data.orderNo")"
api_post device_order_confirm_paid "/api/devices/orders/${CONFIRM_DEVICE_ORDER_NO}/mock-success" "{}"
api_post device_order_speaker /api/devices/orders "{\"banquetId\":${BANQUET_ID},\"deviceType\":\"CLOUD_SPEAKER\",\"rentStartAt\":\"2026-10-01T17:00:00\",\"rentEndAt\":\"2026-10-01T22:00:00\",\"deliveryMethod\":\"${DEVICE_DELIVERY}\"}"
SPEAKER_DEVICE_ORDER_NO="$(json_get device_order_speaker "data.data.orderNo")"
api_post device_order_speaker_paid "/api/devices/orders/${SPEAKER_DEVICE_ORDER_NO}/mock-success" "{}"

api_post confirm_bind /api/confirm-screen/bind "{\"banquetId\":${BANQUET_ID},\"bindCode\":\"${BIND_CODE}\"}"

api_post online_gift_order /api/gifts/payment-orders "{\"banquetId\":${BANQUET_ID},\"entrySource\":\"ONLINE_GIFT\",\"guestName\":\"演示来宾-王五\",\"amount\":520.00,\"blessing\":\"百年好合，喜乐长久\",\"payerOpenId\":\"demo-online-${RUN_ID}\"}"
ONLINE_GIFT_ORDER_NO="$(json_get online_gift_order "data.data.order.orderNo")"
api_post online_gift_success "/api/gifts/payment-orders/${ONLINE_GIFT_ORDER_NO}/mock-success" "{}"

api_post onsite_gift_order /api/gifts/payment-orders "{\"banquetId\":${BANQUET_ID},\"entrySource\":\"ONSITE_QR\",\"guestName\":\"演示来宾-赵六\",\"amount\":299.00,\"blessing\":\"现场祝福，万事顺遂\",\"payerOpenId\":\"demo-onsite-${RUN_ID}\"}"
ONSITE_GIFT_ORDER_NO="$(json_get onsite_gift_order "data.data.order.orderNo")"
api_post onsite_gift_success "/api/gifts/payment-orders/${ONSITE_GIFT_ORDER_NO}/mock-success" "{}"

api_post offline_gift /api/gifts/offline "{\"banquetId\":${BANQUET_ID},\"guestName\":\"演示来宾-钱七\",\"amount\":666.00,\"blessing\":\"现金记礼，祝福满满\"}"
api_post manual_favor /api/favor/manual "{\"contactName\":\"演示来宾-王五\",\"direction\":\"GIVEN\",\"amount\":200.00,\"note\":\"历史回礼演示数据\"}"

WANGWU_Q="$(url_encode "演示来宾-王五")"
api_get public_invitation "/api/invitations/public/${SHARE_SLUG}"
api_get rsvp_stats "/api/rsvp/stats?banquetId=${BANQUET_ID}"
api_get gift_summary "/api/gifts/summary?banquetId=${BANQUET_ID}"
api_get favor_compare "/api/favor/compare?contactName=${WANGWU_Q}"
api_get confirm_latest_event "/api/confirm-screen/banquets/${BANQUET_ID}/latest-event"
api_get admin_banquet "/api/admin/banquets/${BANQUET_ID}" "${AUTH_HEADER[@]}"

cat > "${WORK_DIR}/summary.json" <<EOF
{
  "demoKey": "${DEMO_KEY}",
  "runId": "${RUN_ID}",
  "baseUrl": "${BASE_URL}",
  "banquetId": ${BANQUET_ID},
  "invitationId": ${INVITATION_ID},
  "shareSlug": "${SHARE_SLUG}",
  "publicInvitationUrl": "${BASE_URL}/api/invitations/public/${SHARE_SLUG}",
  "confirmScreenBindCode": "${BIND_CODE}",
  "planOrderNo": "${PLAN_ORDER_NO}",
  "confirmDeviceOrderNo": "${CONFIRM_DEVICE_ORDER_NO}",
  "speakerDeviceOrderNo": "${SPEAKER_DEVICE_ORDER_NO}",
  "onlineGiftOrderNo": "${ONLINE_GIFT_ORDER_NO}",
  "onsiteGiftOrderNo": "${ONSITE_GIFT_ORDER_NO}",
  "artifacts": "${WORK_DIR}"
}
EOF

echo "Demo data seeded."
echo "Artifacts: ${WORK_DIR}"
echo "Banquet ID: ${BANQUET_ID}"
echo "Invitation share slug: ${SHARE_SLUG}"
echo "Public invitation API: ${BASE_URL}/api/invitations/public/${SHARE_SLUG}"
echo "Confirm screen bind code: ${BIND_CODE}"
echo "Plan order: ${PLAN_ORDER_NO}"
echo "Confirm-screen device order: ${CONFIRM_DEVICE_ORDER_NO}"
echo "Cloud-speaker device order: ${SPEAKER_DEVICE_ORDER_NO}"
echo "Online gift order: ${ONLINE_GIFT_ORDER_NO}"
echo "Onsite QR gift order: ${ONSITE_GIFT_ORDER_NO}"
