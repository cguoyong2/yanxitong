# Yanxitong MVP Delivery Package

## Delivery Scope

This package covers the current MVP implementation for Yanxitong:

- Platform admin login and configuration center.
- Banquet creation, scene themes and base public invitation page.
- Plan rights, lightweight device rental and device binding.
- RSVP, unified online gift payment, onsite QR payment, offline cash gift records and favor ledger.
- Confirm screen, WebSocket gift-success events and simulated cloud speaker logs.
- CSV/XLSX export for RSVP, gift records and favor ledger under the `EXCEL_EXPORT` entitlement.
- Smoke-testable local runtime with MySQL, Redis, backend, admin, miniapp and confirm-screen projects.

## Final Delivery Status

The current codebase is ready for local MVP product and technical acceptance. The preferred verification path is the one-command local acceptance script:

```bash
bash deploy/scripts/local-acceptance.sh
```

That script starts missing local services, runs backend smoke, seeds demo data, runs admin browser smoke, runs confirm-screen browser smoke for desktop/mobile viewports, and writes a consolidated report at:

```text
${ARTIFACTS_ROOT:-$TMPDIR/yanxitong-local-acceptance-<runId>}/summary.json
```

The consolidated report includes child artifacts directories, frontend smoke summaries, confirm-screen screenshot paths and failure tails when any step fails.

The latest acceptance conclusion is recorded in `docs/mvp-acceptance-conclusion.md`.

The recommended P1 roadmap is recorded in `docs/p1-roadmap.md`.

## Runtime Modules

| Module | Path | Purpose |
| --- | --- | --- |
| Backend | `server` | Spring Boot API, migrations, auth, business services, WebSocket |
| Admin | `admin` | Platform operation admin UI |
| Miniapp | `miniapp` | User-facing banquet, invitation, RSVP, gift and ledger pages |
| Confirm screen | `confirm-screen` | H5/Web display for gift-success confirmation |
| Docs | `docs` | API, architecture and delivery notes |
| Deploy | `deploy` | Docker Compose and smoke scripts |

## Implemented Functional Checklist

### Platform Admin And Configuration

- Admin login with default account `admin` / `admin123`.
- Tenant context and token-based admin authentication.
- Config item management.
- Event type management.
- Theme management.
- Theme copywriting management.
- Plan and plan-right management.
- Template type and invitation-template management.
- Device config management.
- Operation log list.
- Broadcast log list with filters by banquet, device type and status.

### Banquet, Theme And Invitation

- Banquet creation from configured event type.
- `banquet.theme_code` is retained and resolved from event type default theme.
- Theme copywriting priority:
  1. banquet custom copywriting
  2. theme copywriting
  3. event type default copywriting
  4. system default copywriting
- Base public invitation page for sharing with lightweight template style rendering.
- Basic invitation edit: title, host name, cover URL and greeting.
- Invitation visit logging.

### Plan Rights And Devices

- Active plan list.
- Plan order creation.
- Duplicate plan-order submit returns the existing order.
- Mock plan payment activation.
- Banquet-level entitlement query.
- Banquet-level right check.
- Device config list.
- Device order creation after entitlement check.
- Duplicate device-order submit returns the existing order.
- Mock device payment confirmation.
- Confirm screen binding by `banquetId` and `bindCode`; real hardware SN is not required in MVP.

### RSVP, Gifts And Favor Ledger

- RSVP submit, list and stats.
- Repeated RSVP submit updates the existing guest record.
- RSVP stats include total, attending, pending, declined, guests, meal and accommodation counts.
- Online gift and onsite QR share one payment-order flow.
- Mock gift payment success writes payment callback, gift record, favor entry and broadcast logs.
- Offline cash gift writes gift record, favor entry and broadcast logs without a payment order.
- Gift list supports source and guest-name filters.
- Gift summary returns source counts and source amounts.
- Favor ledger automatically records received gifts.
- Manual favor entry supports `RECEIVED` and `GIVEN`.
- Contact search and real-time bilateral comparison.
- No `favor_compare_snapshot` table is used.

### Confirm Screen And Cloud Speaker

- Confirm screen bind page.
- Standby page supports on-site large-screen visuals, WebSocket connection state, and latest gift event display.
- Success page for gift paid event with amount focus and return countdown.
- Offline page for reconnect/rebind and latest-event recovery guidance.
- Status API includes online state and online session count.
- Latest event API supports refresh/reconnect recovery.
- Cloud speaker logs are simulated in `broadcast_log`.
- Confirm-screen push results are logged as `PUSHED` or `OFFLINE`.

### Export Boundary

- Admin CSV/XLSX export endpoints for gift records, RSVP records and favor entries.
- Export requires banquet-level `EXCEL_EXPORT` entitlement.
- Export writes generic operation logs under module `EXPORT`.
- Current writer is synchronous and banquet-scoped; larger datasets still need row caps, streaming or async delivery before production volume grows.

### Payment Provider Hardening

- Payment providers are configured through `payment.*` application properties and environment variables.
- Admin provider status endpoint exposes enablement and masked merchant/certificate fields without returning secrets.
- Mock callbacks use HMAC signature verification in local/demo configuration. Mock success endpoints are disabled unless `PAYMENT_MOCK_SUCCESS_ENABLED=true`.
- Invalid signatures are recorded as failed callback logs before business fulfillment.
- WeChat service-provider adapter remains a reserved production boundary for official API and certificate verification.

## API Summary

### Health And Auth

- `GET /api/health`
- `GET /api/health/readiness`
- `POST /api/auth/login`

### Admin Configuration

The following admin endpoints support list, save and delete through the shared admin CRUD controller:

- `/api/admin/config-items`
- `/api/admin/plans`
- `/api/admin/plan-rights`
- `/api/admin/event-types`
- `/api/admin/themes`
- `/api/admin/theme-copywriting`
- `/api/admin/template-types`
- `/api/admin/invitation-templates`
- `/api/admin/device-configs`

Additional admin endpoints:

- `GET /api/admin/banquets`
- `GET /api/admin/banquets/{id}`
- `GET /api/admin/orders/plans`
- `GET /api/admin/orders/devices`
- `GET /api/admin/operation-logs`
- `GET /api/admin/broadcast-logs`
- `GET /api/admin/payments/providers`
- `GET /api/admin/exports/banquets/{banquetId}/gifts.csv`
- `GET /api/admin/exports/banquets/{banquetId}/rsvp.csv`
- `GET /api/admin/exports/banquets/{banquetId}/favor.csv`
- `GET /api/admin/exports/banquets/{banquetId}/gifts.xlsx`
- `GET /api/admin/exports/banquets/{banquetId}/rsvp.xlsx`
- `GET /api/admin/exports/banquets/{banquetId}/favor.xlsx`

### Public Metadata

- `GET /api/meta/event-types`
- `GET /api/meta/invitation-templates`

### Banquet And Invitation

- `GET /api/banquets`
- `POST /api/banquets`
- `GET /api/banquets/{id}`
- `GET /api/invitations/public/{shareSlug}`
- `PUT /api/invitations/{id}/basic`

### Plans And Rights

- `GET /api/plans`
- `POST /api/plans/orders`
- `POST /api/plans/orders/{orderNo}/mock-success`
- `GET /api/plans/{planId}/rights/check?rightCode=...`
- `GET /api/plans/banquets/{banquetId}/entitlements`
- `GET /api/plans/banquets/{banquetId}/rights/check?rightCode=...`

### Devices And Confirm Screen

- `GET /api/devices/configs`
- `POST /api/devices/orders`
- `GET /api/devices/orders?banquetId=...`
- `POST /api/devices/orders/{orderNo}/mock-success`
- `POST /api/confirm-screen/bind`
- `GET /api/confirm-screen/status/{bindCode}`
- `GET /api/confirm-screen/banquets/{banquetId}/latest-event`
- `WS /ws/confirm-screen?banquetId=...`

### RSVP, Gifts And Favor

- `POST /api/rsvp/submit`
- `GET /api/rsvp/list?banquetId=...`
- `GET /api/rsvp/stats?banquetId=...`
- `GET /api/gifts?banquetId=...`
- `GET /api/gifts?banquetId=...&source=...&keyword=...`
- `GET /api/gifts/summary?banquetId=...`
- `POST /api/gifts/payment-orders`
- `POST /api/gifts/payment-orders/{orderNo}/mock-success`
- Gift payment creation supports optional `clientRequestId` idempotency and returns the existing payment order when the same client request is retried.
- `POST /api/gifts/offline`
- `GET /api/favor/contacts`
- `GET /api/favor/contacts?keyword=...`
- `GET /api/favor/contacts/{id}`
- `GET /api/favor/compare?contactName=...`
- `POST /api/favor/manual`

## Database Tables

### Base And Auth

- `tenant`
- `admin_user`
- `role`
- `admin_user_role`

### Config And Rights

- `config_item`
- `plan`
- `plan_right`

### Theme And Copywriting

- `event_type`
- `theme`
- `theme_copywriting`

### Invitation And Banquet

- `template_type`
- `invitation_template`
- `invitation`
- `invitation_share`
- `invitation_visit_log`
- `banquet`
- `rsvp_record`

### Orders And Devices

- `plan_order`
- `device_config`
- `device_order`
- `device`
- `device_bind`

### Payment, Gift, Favor And Broadcast

- `payment_order`
- `payment_callback_log`
- `gift_record`
- `favor_contact`
- `favor_entry`
- `broadcast_log`

### Audit

- `operation_log`

## Seed Data

Migrations seed:

- Default tenant.
- MVP config items.
- Wedding, birthday, school, memorial and general themes/event types.
- Theme copywriting for gift-success scenes.
- `BASIC`, `PRO`, `PREMIUM` plans.
- `EXCEL_EXPORT` entitlement for `PRO` and `PREMIUM`; current implementation exposes CSV/XLSX downloads behind that boundary.
- Device rental, confirm-screen and cloud-speaker rights for `PRO` and `PREMIUM`.
- Free, paid and custom invitation template presets with public-page and selection-card rendering.
- Cloud speaker and confirm-screen device configs.
- Default admin user.

## Local Startup

Start MySQL and Redis:

```bash
docker compose -f deploy/docker-compose.yml up -d
```

Build backend:

```bash
cd server
DB_URL='jdbc:mysql://127.0.0.1:3308/yanxitong?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
DB_USERNAME=yanxitong \
DB_PASSWORD=yanxitong \
REDIS_HOST=127.0.0.1 \
REDIS_PORT=6381 \
mvn clean package -DskipTests
```

Run backend:

```bash
DB_URL='jdbc:mysql://127.0.0.1:3308/yanxitong?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
DB_USERNAME=yanxitong \
DB_PASSWORD=yanxitong \
REDIS_HOST=127.0.0.1 \
REDIS_PORT=6381 \
java -jar target/server-0.0.1-SNAPSHOT.jar
```

Run admin:

```bash
cd admin
npm run dev
```

Run confirm screen:

```bash
cd confirm-screen
npm run dev
```

Build miniapp:

```bash
cd miniapp
npm run build
```

## Automated Verification

Preferred full local acceptance:

```bash
bash deploy/scripts/local-acceptance.sh
```

This verifies backend business behavior, seeds demo data, checks the admin UI with a banquet deep link, and checks confirm-screen bind/standby/success/offline pages in desktop and mobile viewports.

After backend starts:

```bash
bash deploy/scripts/smoke-test.sh
```

The smoke test verifies:

- Health check.
- Admin login and protected admin endpoint.
- Config center write paths.
- Banquet creation, theme resolution and invitation update.
- RSVP repeat-submit idempotency and stats.
- Plan rights, duplicate plan order and mock activation.
- Device right blocking, duplicate device order and mock confirmation.
- Confirm-screen bind, status and latest gift event.
- Online gift, onsite QR and offline cash gift.
- Gift summary and filters.
- Favor search and bilateral comparison.
- Broadcast logs for cloud speaker and confirm screen.
- Operation logs.

After backend and admin frontend start:

```bash
bash deploy/scripts/admin-frontend-smoke.sh
```

After backend and confirm-screen frontend start:

```bash
bash deploy/scripts/confirm-screen-smoke.sh
```

For demo-friendly data only:

```bash
bash deploy/scripts/seed-demo-data.sh
```

## Manual Acceptance Flow

1. Log into admin with `admin` / `admin123`.
2. Review config center, themes, event types, plans, templates and device configs.
3. Create a banquet from admin or miniapp.
4. Open banquet detail and public invitation.
5. Edit base invitation fields.
6. Submit RSVP twice with the same guest and verify stats update rather than duplicate.
7. Open version page, create a `PRO` order, mock payment and verify device rights.
8. Create a device order and mock device payment.
9. Bind confirm screen with `banquetId` and a bind code.
10. Create online gift and onsite QR gift orders and mock payment success.
11. Record an offline cash gift.
12. Verify gift list, gift summary, favor ledger, favor comparison and broadcast logs.
13. Open confirm-screen standby and verify latest event and WebSocket behavior.

## Explicit MVP Boundaries

- CSV/XLSX export is enabled for RSVP, gift records and favor ledger under the `EXCEL_EXPORT` entitlement; production volume hardening remains a later enhancement behind the same export boundary.
- Agent, hotel and wedding-company workspaces are not implemented.
- Complex visual invitation editor is not implemented.
- Device inventory scheduling, deposit, repair and return flows are not implemented.
- Real cloud speaker hardware integration is not implemented.
- Real payment provider API calls are behind the adapter boundary; local mock callbacks now require HMAC signature verification when using the callback endpoint.
- `favor_compare_snapshot` is not created; comparisons are computed from `favor_entry`.
- `confirm_screen_event` is not created; confirmation events are derived from successful gift records and logged in `broadcast_log`.

## Delivery Status

The MVP implementation is ready for local product and technical acceptance using `bash deploy/scripts/local-acceptance.sh`, the individual smoke scripts and the manual flow above.
