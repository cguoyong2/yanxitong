# Scripts

## Local Acceptance

`local-acceptance.sh` runs the local MVP acceptance chain in one command. It starts Docker infrastructure by default, starts backend/admin/confirm-screen services when they are not already reachable, runs backend smoke, seeds demo data, then runs admin and confirm-screen frontend smoke.

```bash
bash deploy/scripts/local-acceptance.sh
```

Environment variables:

- `ARTIFACTS_ROOT`: optional output directory for logs and summary
- `BASE_URL`: backend API base URL, default `http://127.0.0.1:8080`
- `ADMIN_URL`: admin frontend URL, default `http://127.0.0.1:5173`
- `CONFIRM_SCREEN_URL`: confirm-screen frontend URL, default `http://127.0.0.1:5174`
- `DB_URL`: backend database URL, default points to Docker MySQL on `127.0.0.1:3308`
- `DB_PORT`: optional infrastructure port check override; defaults to the port parsed from `DB_URL`
- `DB_USERNAME` / `DB_PASSWORD`: default `yanxitong` / `yanxitong`
- `REDIS_HOST` / `REDIS_PORT`: default `127.0.0.1` / `6381`
- `LOCAL_ACCEPTANCE_SKIP_DOCKER`: set to `1` to skip `docker compose up -d`
- `CHROME_PATH`: optional Chrome/Chromium executable path for frontend smoke
- `HEADLESS`: set to `0` to show the browser

Coverage:

- backend MVP smoke
- demo data seed for browser acceptance
- admin frontend smoke with banquet deep link
- confirm-screen frontend smoke with desktop/mobile screenshots
- logs for services started by the script
- consolidated `summary.json` with child artifacts directories, frontend smoke summaries, screenshot paths and failure tails

## Production Preflight

`production-preflight.sh` checks production environment variables, mock-payment gates, frontend build artifacts, and optionally remote health/readiness endpoints.

```bash
SKIP_REMOTE_CHECKS=1 ENV_FILE=deploy/.env.production bash deploy/scripts/production-preflight.sh
BASE_URL=https://example.com ENV_FILE=deploy/.env.production bash deploy/scripts/production-preflight.sh
```

Environment variables:

- `ENV_FILE`: production env file path, default `deploy/.env.production`
- `BASE_URL`: public deployment base URL, default `http://127.0.0.1`
- `SKIP_REMOTE_CHECKS`: set to `1` before services are reachable
- `REQUIRE_READINESS_READY`: default `1`

## Smoke Test

`smoke-test.sh` verifies the MVP chain after the backend is running.

```bash
bash deploy/scripts/smoke-test.sh
```

Environment variables:

- `BASE_URL`: backend API base URL, default `http://127.0.0.1:8080`
- `ADMIN_USERNAME`: default `admin`
- `ADMIN_PASSWORD`: default `admin123`

Coverage:

- health check and admin auth
- config center writes
- banquet creation, theme resolution and invitation update
- RSVP idempotency and stats
- plan rights, device entitlement and device order flow
- confirm-screen status and latest event
- online gift, onsite QR, offline cash gift
- gift summary, favor search and comparison
- broadcast logs and operation logs

## Demo Data Seed

`seed-demo-data.sh` creates a repeatable demo dataset after the backend is running.

```bash
bash deploy/scripts/seed-demo-data.sh
```

Environment variables:

- `BASE_URL`: backend API base URL, default `http://127.0.0.1:8080`
- `ADMIN_USERNAME`: default `admin`
- `ADMIN_PASSWORD`: default `admin123`
- `DEMO_KEY`: named fixture key, default `standard`

Behavior:

- Configuration fixtures are reused by `DEMO_KEY` where possible.
- Business fixtures create a fresh banquet loop for each run.
- The script prints banquet ID, invitation share slug, confirm-screen bind code and related order numbers.

Created data:

- demo event type, theme and gift-success copywriting
- confirm-screen and cloud-speaker device configs
- banquet and base invitation
- RSVP attending and declined guests
- activated plan order
- paid confirm-screen and cloud-speaker device orders
- confirm-screen binding
- online gift, onsite QR gift and offline cash gift
- manual favor entry for bilateral comparison

## Admin Frontend Smoke

`admin-frontend-smoke.sh` checks the admin UI after the backend and admin frontend are running.

```bash
bash deploy/scripts/admin-frontend-smoke.sh
```

Environment variables:

- `ADMIN_URL`: admin frontend URL, default `http://127.0.0.1:5173`
- `ADMIN_USERNAME`: default `admin`
- `ADMIN_PASSWORD`: default `admin123`
- `BANQUET_ID`: optional banquet ID for deep-linking the banquet detail drawer
- `CHROME_PATH`: optional Chrome/Chromium executable path
- `HEADLESS`: set to `0` to show the browser
- `ARTIFACTS_DIR`: optional output directory for `summary.json` and failure screenshots

Coverage:

- admin login
- dashboard and configuration pages
- banquet detail page, optionally with `BANQUET_ID`
- business data, orders, payments, broadcast logs and operation logs
- non-empty page render
- primary text presence
- document-level horizontal overflow
- obvious API/request failures
- raw backend enum leakage in visible page text

## Confirm Screen Frontend Smoke

`confirm-screen-smoke.sh` checks the confirm-screen UI after the backend and confirm-screen frontend are running.

```bash
bash deploy/scripts/confirm-screen-smoke.sh
```

Environment variables:

- `BASE_URL`: backend API base URL, default `http://127.0.0.1:8080`
- `CONFIRM_SCREEN_URL`: confirm-screen frontend URL, default `http://127.0.0.1:5174`
- `BANQUET_ID`: optional banquet ID; when omitted, the script seeds demo data
- `CONFIRM_SCREEN_BIND_CODE`: optional bind code; when omitted, the script seeds demo data
- `DEMO_KEY`: seed fixture key when demo data is needed, default `confirm-screen-smoke`
- `CHROME_PATH`: optional Chrome/Chromium executable path
- `HEADLESS`: set to `0` to show the browser
- `ARTIFACTS_DIR`: optional output directory for `summary.json` and screenshots

Coverage:

- bind page render
- standby page URL binding override and latest gift event display
- success page navigation from latest gift event
- offline page recovery state
- desktop and mobile viewport screenshots
- non-empty page render
- document-level horizontal overflow
- obvious frontend request failures
