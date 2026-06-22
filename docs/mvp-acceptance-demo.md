# MVP Acceptance And Demo Script

This document is the working checklist for MVP acceptance and product demo. It complements `docs/mvp-delivery-package.md`, which records the implemented scope and API surface.

## Acceptance Goal

The MVP should prove one complete Yanxitong business loop:

1. Platform admin can maintain prices, themes, templates, devices and rights without hard-coding frontend data.
2. An operator can create a banquet from a configured event type and theme.
3. The banquet has a shareable base invitation/public page.
4. The host can buy a plan, pass rights checks and optionally rent confirm-screen/cloud-speaker devices.
5. Guests can RSVP and give gifts through one unified online payment flow.
6. Offline cash gifts can be recorded and written into the same gift/favor ledger.
7. Gift success can drive confirm-screen display and simulated cloud-speaker broadcast logs.
8. Admin can review operation logs, orders, payments, RSVP, gifts, favor ledger and broadcast records.

## Acceptance Preconditions

### Local Infrastructure

- MySQL is available on `127.0.0.1:3308`.
- Redis is available on `127.0.0.1:6381`.
- Backend starts with the local `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST` and `REDIS_PORT` environment variables in `README.md`.
- Admin app starts on Vite.
- Confirm-screen app starts on Vite when validating the realtime display flow.
- Default admin account is `admin` / `admin123`.

### Verification Commands

Preferred full local acceptance before formal demo:

```bash
bash deploy/scripts/local-acceptance.sh
```

This command starts missing local services, runs backend smoke, seeds demo data, runs admin browser smoke and runs confirm-screen desktop/mobile smoke. Use the generated `summary.json` as the demo readiness report.

Run lower-level checks when debugging a specific layer:

```bash
cd server
mvn -q test
mvn -q clean package -DskipTests
```

```bash
cd ..
bash deploy/scripts/smoke-test.sh
```

For frontend packaging:

```bash
cd admin
npm run build
cd ../confirm-screen
npm run build
```

For admin UI smoke checks after backend and admin frontend are running:

```bash
bash deploy/scripts/admin-frontend-smoke.sh
```

When demo data has been seeded, pass the banquet ID for the banquet detail deep link:

```bash
BANQUET_ID=<id> bash deploy/scripts/admin-frontend-smoke.sh
```

For backend CSV export verification, the regular smoke script downloads gift, RSVP and favor CSV files after activating a plan with `EXCEL_EXPORT`:

```bash
bash deploy/scripts/smoke-test.sh
```

For confirm-screen visual and realtime page smoke after backend and confirm-screen frontend are running:

```bash
bash deploy/scripts/confirm-screen-smoke.sh
```

## Demo Path

### Step 1: Admin Login And Dashboard

Goal: prove platform operation admin is the only first-stage backend user.

Demo actions:

- Open admin login.
- Log in with `admin` / `admin123`.
- Enter dashboard and confirm core metrics can load.

Acceptance points:

- Login succeeds with token-based auth.
- Admin routes are protected before login.
- Dashboard is not blocked by missing tenant context.

### Step 2: Configuration Center

Goal: prove price, unit, event type, theme, template and device data are backend-maintained.

Demo actions:

- Open config item management.
- Open event type, theme and theme copywriting management.
- Open template type and invitation template management.
- Open plan, plan-right and device config management.

Acceptance points:

- Records can be listed from backend data.
- CRUD entry points exist for MVP configuration resources.
- Template management is limited to type, list, cover, price type, status and sort.
- Complex visual template editor is not part of MVP.
- Theme copywriting is configured separately from banquet records.

### Step 3: Banquet Creation And Scene Theme

Goal: prove banquet creation is driven by configured event type and theme.

Demo actions:

- Create a banquet from the miniapp flow or use the smoke-generated banquet.
- Choose an event type.
- Confirm banquet detail includes event type, `theme_code`, banquet time and public invitation.

Acceptance points:

- `banquet.theme_code` is retained.
- Event type can decide the default theme.
- Theme copywriting fallback priority is:
  1. banquet custom copywriting
  2. theme copywriting
  3. event type default copywriting
  4. system default copywriting
- Base invitation/public page is available for sharing.

### Step 4: Base Invitation/Public Page

Goal: prove the banquet has a guest-facing entrance before complex template editing exists.

Demo actions:

- Open the public invitation URL by share slug.
- View basic banquet information, host information, cover and greeting.
- Submit RSVP from the invitation flow.
- Enter gift/payment and device selection entry points from the public page or miniapp pages.

Acceptance points:

- Invitation instance exists independently from template config.
- Template reference is preserved for later template rendering iteration.
- Template reference now affects the base public page style and cover fallback.
- Template selection uses preview cards in the banquet creation flow.
- Basic fields can be edited.
- Full visual invitation editor remains later work.

### Step 5: Plan Rights

Goal: prove rights are checked before paid features and device rental.

Demo actions:

- Create a plan order for the banquet.
- Trigger mock plan payment success.
- Open entitlement query or admin banquet detail.
- Check device-related rights.

Acceptance points:

- Duplicate plan-order submit is idempotent.
- Payment success activates banquet-level entitlement.
- Rights checks are backend-driven.
- Formal export was not required by the original MVP boundary, but the current baseline includes CSV/XLSX downloads behind the `EXCEL_EXPORT` entitlement.

### Step 6: Device Rental

Goal: prove MVP device flow has the required lightweight order loop.

Demo actions:

- Select confirm screen or cloud speaker config.
- Submit device order with rental date/time, price, unit and delivery method.
- Trigger mock device payment success.
- Review device order in admin.

Acceptance points:

- `device_config` and `device_order` are used.
- `device` and `device_bind` have basic structures.
- Confirm screen can bind by `banquetId` and bind code.
- Real hardware SN is not required for MVP.
- No stock scheduling, deposit, repair or return process is included.

### Step 7: RSVP

Goal: prove guest attendance data can be captured and corrected.

Demo actions:

- Submit RSVP for one guest.
- Submit RSVP again with the same guest identity and changed attendance data.
- Open RSVP list and stats.

Acceptance points:

- Repeated RSVP updates the existing guest record.
- Stats do not inflate after duplicate submit.
- Admin can review RSVP records through banquet detail or business data pages.

### Step 8: Unified Online Gift And Onsite QR Payment

Goal: prove online gift and onsite QR are one payment capability with different entrances.

Demo actions:

- Create online gift payment order.
- Create onsite QR gift payment order if demonstrating the separate entry.
- Trigger mock gift payment success.
- Review payment order, callback log, gift record, favor entry and broadcast log.

Acceptance points:

- Both entrances write `payment_order`.
- Both entrances share payment callback handling.
- Gift success writes `gift_record`.
- Gift success writes `favor_entry`.
- Gift success pushes confirm-screen event.
- Gift success writes simulated cloud-speaker/confirm-screen broadcast logs.
- Payment provider access stays behind Provider/Adapter design; business services do not hard-code a concrete payment institution.
- Payment callbacks are verified before fulfillment; invalid signatures are recorded as failed callback logs.

### Step 9: Offline Cash Gift And Favor Ledger

Goal: prove offline gift bookkeeping enters the same ledger without creating a payment order.

Demo actions:

- Record one cash gift.
- Open gift list and gift summary.
- Open favor contact and favor detail.
- Add a manual favor entry.
- Open bilateral comparison.

Acceptance points:

- Offline cash gift writes `gift_record`.
- Offline cash gift writes `favor_entry`.
- Offline cash gift writes broadcast log when applicable.
- Offline cash gift does not create `payment_order`.
- Manual favor entries support received and given directions.
- Bilateral comparison is computed live.
- No `favor_compare_snapshot` table is required in MVP.

### Step 10: Confirm Screen And Cloud Speaker Simulation

Goal: prove realtime display and simulated broadcast are connected to gift success.

Demo actions:

- Bind confirm screen with banquet ID and bind code.
- Open standby page.
- Trigger gift payment success.
- Observe latest gift success page/state.
- Open admin broadcast logs.

Acceptance points:

- Confirm screen supports bind, standby, success and offline states.
- WebSocket receives gift success events.
- Latest event API supports refresh/reconnect recovery.
- Confirm-screen push result is logged.
- Cloud speaker remains simulated through `broadcast_log`.

### Step 11: Admin Operation Review

Goal: prove platform operators can inspect the core business loop after demo data is created.

Demo actions:

- Open banquet detail drawer.
- Review RSVP, gifts, device orders, payment orders and broadcast records.
- Review favor contacts, plan orders and banquet operation logs from the same drawer.
- Open business data pages.
- Open orders, payments and broadcast logs.
- Open operation logs.

Acceptance points:

- Key operations are visible from backend admin.
- `operation_log` is generic, not limited to config changes.
- It covers critical actions such as offline gift recording, favor manual entry, device binding, config changes, template status changes and payment exception handling.

## Completed MVP Scope Matrix

| Area | MVP Status | Acceptance Signal |
| --- | --- | --- |
| Engineering skeleton | Done | Backend, admin, miniapp, confirm-screen and deploy assets exist |
| Database migrations | Done | Flyway migrations cover base, config, banquet, invitation, order, payment, gift, favor, broadcast and operation log tables |
| Redis | Done | Backend starts with Redis config and confirm-screen realtime path |
| Multi-tenant context | Done | Tenant context filter and tenant IDs are present |
| Login/auth | Done | Admin login and protected admin APIs |
| Config center | Done | Config, event type, theme, copywriting, plan, right, template and device config management |
| Banquet creation | Done | Banquet creation stores type/theme/public invitation data |
| Scene theme | Done | Event type default theme and copywriting fallback |
| Base invitation/public page | Done | Share slug public invitation, basic edit, template preview cards and lightweight rendering |
| Plan rights | Done | Plan order, mock payment, entitlement and right check |
| Device rental MVP | Done | Device config, order, payment status and admin review |
| RSVP | Done | Submit, update, list and stats |
| Online gift/onsite QR payment | Done | Shared payment order and callback path |
| Offline cash gift | Done | Gift record and favor entry without payment order |
| Favor ledger | Done | Auto write, manual entry, contact detail and live comparison |
| Confirm screen | Done | Bind, standby, success, offline and latest event |
| Cloud speaker simulation | Done | Broadcast logs generated from gift success |
| Admin business review | Done | Banquet drawer aggregates invitation, RSVP, gifts, favor ledger, plan/device orders, payments, broadcast logs and operation logs |
| Export boundary | Done | Admin CSV/XLSX endpoints for gifts, RSVP and favor ledger with `EXCEL_EXPORT` entitlement |

## MVP Boundaries

The following items are explicitly outside MVP:

- Further export hardening such as async delivery, larger row caps or extended file delivery.
- Agent, hotel or wedding-service independent workspaces.
- Complex visual invitation template editor.
- Full visual invitation editor and complex drag-and-drop template editing.
- Device inventory scheduling.
- Device deposit, repair, return and settlement processes.
- Real hardware SN dependency for confirm-screen binding.
- Production payment institution integration. Adapter boundary is present; concrete production provider wiring is later work.
- `favor_compare_snapshot` persistence.
- Separate `confirm_screen_event` table. Confirm-screen events are derived from successful gift events and can be traced through broadcast logs.

## Demo Data Strategy

Preferred demo data source:

- Use `bash deploy/scripts/local-acceptance.sh` when a full readiness report is needed; it seeds demo data and records the generated banquet ID and bind code in the consolidated `summary.json`.
- Use `bash deploy/scripts/seed-demo-data.sh` to create demo-friendly data and print the banquet ID, share slug, bind code and order numbers.
- Use `DEMO_KEY=<name> bash deploy/scripts/seed-demo-data.sh` when a named fixture namespace is needed.
- Use `bash deploy/scripts/smoke-test.sh` for automated assertion coverage before demo.

Manual demo data should include:

- One banquet with a clear event type and theme.
- One activated plan order.
- One paid device order.
- One RSVP attending guest.
- One online gift.
- One onsite QR gift if separate entrance needs to be shown.
- One offline cash gift.
- One manual favor entry in the opposite direction.
- One confirm-screen binding.

## Demo Risk Checklist

Check these before showing the demo:

- Admin frontend can reach backend API base URL.
- Backend is connected to the intended local MySQL and Redis ports.
- Seed migrations have run.
- Default admin account works.
- `bash deploy/scripts/local-acceptance.sh` passes, or the targeted smoke script for the layer being shown passes.
- The consolidated `summary.json` points to admin and confirm-screen child artifacts.
- Confirm-screen smoke has 8 screenshots: desktop/mobile for bind, standby, success and offline.
- Admin table enum values show Chinese labels instead of raw enum keys.
- Amounts show currency formatting.
- Times do not expose raw ISO `T` formatting in primary admin views.
- Confirm-screen page uses the same banquet ID as the gift payment demo.

## Suggested Demo Order

Use this order for the cleanest story:

1. Admin login.
2. Configuration center: event type, theme, copywriting, plan, template, device config.
3. Create or open banquet.
4. Show base public invitation.
5. Buy plan and verify rights.
6. Rent device and pay device order.
7. Submit RSVP.
8. Submit online gift and mock payment success.
9. Show confirm-screen success and broadcast logs.
10. Record offline cash gift.
11. Show favor ledger and bilateral comparison.
12. Show admin banquet detail and operation logs.

## Next Work After MVP Acceptance

Recommended next work after this checklist is signed off:

1. Replace the WeChat service-provider HMAC placeholder with official certificate/signature verification.
2. Harden export performance, row limits and delivery behavior for larger datasets.
3. Add full invitation template preview pages and richer template presets.
