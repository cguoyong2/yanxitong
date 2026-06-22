# Yanxitong

宴席通 MVP 多目录单仓库。

## Modules

- `server`: Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8 + Redis
- `miniapp`: uni-app + Vue3 + TypeScript + uView Plus
- `admin`: Vue3 + Vite + TypeScript + Element Plus
- `confirm-screen`: Vue3 + Vite + TypeScript independent H5/Web screen
- `docs`: product, architecture, API, database and change notes
- `deploy`: Docker, Nginx, MySQL, Redis deployment assets

## MVP Order

1. Engineering skeleton and configuration center
2. Banquet creation, scene theme and base public invitation page
3. Plan rights and lightweight device order flow
4. RSVP, unified online gift payment, gift records and favor ledger
5. Confirm screen and simulated cloud speaker logs

## Local Run

Start infrastructure:

```bash
docker compose -f deploy/docker-compose.yml up -d
```

Start backend:

```bash
cd server
DB_URL='jdbc:mysql://127.0.0.1:3308/yanxitong?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
DB_USERNAME=yanxitong \
DB_PASSWORD=yanxitong \
REDIS_HOST=127.0.0.1 \
REDIS_PORT=6381 \
mvn clean package -DskipTests

DB_URL='jdbc:mysql://127.0.0.1:3308/yanxitong?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
DB_USERNAME=yanxitong \
DB_PASSWORD=yanxitong \
REDIS_HOST=127.0.0.1 \
REDIS_PORT=6381 \
java -jar target/server-0.0.1-SNAPSHOT.jar
```

Start admin:

```bash
cd admin
npm run dev
```

Start confirm screen:

```bash
cd confirm-screen
npm run dev
```

Default admin account:

- username: `admin`
- password: `admin123`

See `docs/admin-auth.md` for the MVP authentication boundary.

For a consolidated delivery view, see `docs/mvp-delivery-package.md`.

For business acceptance and demo sequencing, see `docs/mvp-acceptance-demo.md`.

For the latest local MVP acceptance conclusion, see `docs/mvp-acceptance-conclusion.md`.

For the MVP release-readiness gates, see `docs/mvp-release-readiness.md`.

For the recommended P1 roadmap, see `docs/p1-roadmap.md`.

For P1-A payment production design, see `docs/p1-payment-production-design.md`.

For production operation steps, see `docs/production-operations-runbook.md`.

For public entry rate-limit and abuse-protection boundaries, see `docs/public-entry-security.md`.

For the minimum production deployment loop, see `docs/production-deployment.md`.

Run the full local acceptance chain:

```bash
bash deploy/scripts/local-acceptance.sh
```

This command starts missing local services, runs backend smoke, seeds demo data, runs admin browser smoke, runs confirm-screen desktop/mobile smoke and writes a consolidated `summary.json`.

Run the release preflight chain:

```bash
bash deploy/scripts/release-readiness.sh
```

This command runs backend tests, admin build, confirm-screen build, miniapp build and records `/api/health/readiness` into a consolidated `summary.json`. Use `REQUIRE_READINESS_READY=1` when checking a production-like environment.

Run backend smoke only after backend starts:

```bash
bash deploy/scripts/smoke-test.sh
```

Check production safety readiness:

```bash
curl http://127.0.0.1:8080/api/health/readiness
```

Before production, this check must report `status=READY`. It detects local defaults such as `admin/admin123`, `DB_PASSWORD=yanxitong`, blank Redis password, the default mock callback secret, `PAYMENT_MOCK_SUCCESS_ENABLED=true` and `PAYMENT_DEFAULT_PROVIDER=MOCK`.

## MVP Acceptance Checklist

- Admin can log in with `admin` / `admin123`.
- Config center can maintain event types, themes, theme copywriting, plans, templates and device configs.
- Creating a banquet picks event type, default theme, template and creates a public invitation.
- Basic invitation fields can be edited and public invitation can enter RSVP, gift payment and device selection.
- Plan order supports duplicate-click idempotency and mock payment activation.
- Device order is blocked before plan rights, allowed after plan activation, and duplicate-click idempotent.
- Online gift and onsite QR use the same payment order path; mock success writes gift record, favor entry, confirm-screen event and broadcast logs.
- Offline cash gift writes gift record, favor entry and simulated broadcast logs without creating a payment order.
- RSVP repeat submission updates the same guest record instead of inflating attendance statistics.
- Favor ledger supports manual entries, contact search and real-time bilateral comparison without `favor_compare_snapshot`.
- Confirm screen can bind by `banquetId` and `bindCode`, detect online status, read latest gift event and receive WebSocket events.
- Admin broadcast logs can filter cloud speaker and confirm-screen logs by banquet, device type and status.
- Admin CSV/XLSX export can download gift, RSVP and favor ledger data for banquets with `EXCEL_EXPORT` entitlement.
- Payment callbacks are verified through the Provider/Adapter boundary; local Mock callbacks use HMAC signatures.

## Hardening Boundaries

- MVP mock payment endpoints are for local verification only and are disabled by default. Set `PAYMENT_MOCK_SUCCESS_ENABLED=true` only for local demo or acceptance runs; real provider API calls remain behind the payment adapter boundary.
- CSV/XLSX export is implemented behind the `EXCEL_EXPORT` boundary; production use still needs row caps and entitlement checks.
- Device flow remains lightweight: no stock scheduling, deposit, repair or return workflow.
- Confirm screen binding does not require a real hardware SN in MVP.
