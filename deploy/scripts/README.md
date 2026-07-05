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
- `CONFIRM_SCREEN_URL`: confirm-screen frontend URL, default `http://127.0.0.1:5174/confirm-screen`
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

## Production Acceptance Suite

`production-acceptance-suite.sh` is the unified launch-check entrypoint. It writes one consolidated `summary.json` while reusing the existing preflight, release-readiness, production API acceptance, browser smoke, ops/security checks and miniapp preview scripts.

```bash
bash deploy/scripts/production-acceptance-suite.sh
```

Remote production checks are opt-in so the suite can be used before all credentials are available:

```bash
ADMIN_PASSWORD='<admin-password>' \
BASE_URL=https://yxt.yqej.cn \
SKIP_REMOTE_CHECKS=0 \
RUN_PRODUCTION_API=1 \
RUN_PRODUCTION_BROWSER=1 \
RUN_OPS_CHECK=1 \
bash deploy/scripts/production-acceptance-suite.sh
```

Environment variables:

- `ARTIFACTS_ROOT`: output directory, default `.artifacts/production-acceptance/<run-id>`
- `BASE_URL`: public deployment base URL, default `https://yxt.yqej.cn`
- `ENV_FILE`: production env file, default `deploy/.env.production`
- `RUN_PREFLIGHT`: default `1`
- `RUN_RELEASE_READINESS`: default `1`
- `RUN_ADMIN_SOURCE_SMOKE`: default `1`
- `RUN_PRODUCTION_API`: default `0`, requires `ADMIN_PASSWORD`
- `RUN_PRODUCTION_BROWSER`: default `0`, requires `ADMIN_PASSWORD` and Chrome
- `RUN_OPS_CHECK`: default `0`, requires SSH access
- `RUN_SECURITY_CHECK`: default `0`, requires SSH access
- `RUN_MINIAPP_PREVIEW`: default `0`, requires WeChat DevTools CLI login
- `SKIP_REMOTE_CHECKS`: passed to production preflight, default `1`
- `REQUIRE_READINESS_READY`: default `0` before real WeChat payment launch
- `SKIP_MINIAPP_BUILD`: passed to release readiness, default `0`

The generated `summary.json` records each step status, log path and the last lines of failed logs.

## Production API Acceptance

`production-api-acceptance.sh` verifies the production API loop without calling mock-success endpoints. It is safe to run while real payment launch is still blocked because it does not force payment completion.

```bash
ADMIN_PASSWORD='<admin-password>' BASE_URL=https://yxt.yqej.cn bash deploy/scripts/production-api-acceptance.sh
```

Environment variables:

- `BASE_URL`: public deployment base URL, default `https://yxt.yqej.cn`
- `ADMIN_USERNAME`: default `admin`
- `ADMIN_PASSWORD`: required
- `ARTIFACTS_DIR`: optional output directory for response payloads and `summary.json`

Coverage:

- health and readiness endpoints
- admin login and protected config read
- event type and invitation template metadata
- banquet and base invitation creation
- public invitation API
- RSVP submission and stats
- offline cash gift and gift summary
- confirm-screen bind/status
- online gift payment-order creation, or controlled failure when readiness is blocked
- admin gift, RSVP, broadcast-log and operation-log list endpoints

## Production Browser Smoke

`production-browser-smoke.sh` verifies the production admin and confirm-screen UI in a browser.

```bash
ADMIN_PASSWORD='<admin-password>' BASE_URL=https://yxt.yqej.cn bash deploy/scripts/production-browser-smoke.sh
```

To also check a specific public invitation API payload, pass a share slug generated by `production-api-acceptance.sh`:

```bash
ADMIN_PASSWORD='<admin-password>' BASE_URL=https://yxt.yqej.cn SHARE_SLUG='<share-slug>' bash deploy/scripts/production-browser-smoke.sh
```

Environment variables:

- `BASE_URL`: public deployment base URL, default `https://yxt.yqej.cn`
- `ADMIN_USERNAME`: default `admin`
- `ADMIN_PASSWORD`: required
- `SHARE_SLUG`: optional public invitation share slug
- `CHROME_PATH`: optional Chrome/Chromium executable path
- `HEADLESS`: set to `0` to show the browser
- `ARTIFACTS_DIR`: optional output directory for `summary.json`

Coverage:

- admin login
- dashboard, configuration, banquet, business, payment and log pages
- confirm-screen bind page under `/confirm-screen/`
- optional public invitation API read
- non-empty page render
- document-level horizontal overflow
- console errors and failed frontend/API requests

## Miniapp Preview

`miniapp-preview.sh` runs the miniapp experience check, route check and production build, then asks WeChat DevTools CLI to generate a preview QR code.

```bash
bash deploy/scripts/miniapp-preview.sh
```

Environment variables:

- `WECHAT_CLI`: WeChat DevTools CLI path, default `/Applications/wechatwebdevtools.app/Contents/MacOS/cli`
- `ARTIFACTS_DIR`: output directory, default `.artifacts/wechat-preview`
- `QR_OUTPUT`: QR image path, default `.artifacts/wechat-preview/latest-miniapp-preview.png`
- `INFO_OUTPUT`: preview info JSON path, default `.artifacts/wechat-preview/latest-miniapp-preview.json`
- `SKIP_BUILD`: set to `1` only when `miniapp/dist/build/mp-weixin` is already fresh

Generated preview files are local artifacts and should not be committed.

## Production Database Backup

`production-db-backup.sh` creates a compressed MySQL dump on the production server, plus a SHA-256 checksum and JSON manifest.

```bash
bash deploy/scripts/production-db-backup.sh
```

Environment variables:

- `SSH_TARGET`: SSH target, default `root@115.29.229.188`
- `MYSQL_CONTAINER`: MySQL container name, default `yanxitong-mysql`
- `DATABASE`: database name, default `yanxitong`
- `REMOTE_BACKUP_DIR`: remote backup directory, default `/opt/backups/yanxitong/mysql`
- `BACKUP_BASENAME`: optional backup filename, default `<database>-<timestamp>.sql.gz`
- `RETENTION_DAYS`: optional remote deletion window for old `.sql.gz` files and sidecars
- `LOCAL_COPY_DIR`: optional local directory to copy the `.sql.gz`, `.sha256` and manifest files into

Example with local copy:

```bash
LOCAL_COPY_DIR=/tmp/yanxitong-db-backups bash deploy/scripts/production-db-backup.sh
```

## Production Database Restore

`production-db-restore.sh` restores a remote backup file into a target database. By default it restores into a new `yanxitong_restore_<timestamp>` database so production data is not overwritten.

```bash
BACKUP_FILE=/opt/backups/yanxitong/mysql/yanxitong-20260625181512.sql.gz bash deploy/scripts/production-db-restore.sh
```

Environment variables:

- `SSH_TARGET`: SSH target, default `root@115.29.229.188`
- `MYSQL_CONTAINER`: MySQL container name, default `yanxitong-mysql`
- `BACKUP_FILE`: required remote `.sql.gz` path
- `RESTORE_DATABASE`: restore target, default `yanxitong_restore_<timestamp>`
- `DROP_TARGET_FIRST`: set to `1` to recreate the target database first
- `CONFIRM_RESTORE`: required as `RESTORE_PRODUCTION_YANXITONG` when restoring directly into `yanxitong`

Production overwrite example, only after an explicit rollback decision:

```bash
BACKUP_FILE=/opt/backups/yanxitong/mysql/<backup>.sql.gz \
RESTORE_DATABASE=yanxitong \
DROP_TARGET_FIRST=1 \
CONFIRM_RESTORE=RESTORE_PRODUCTION_YANXITONG \
bash deploy/scripts/production-db-restore.sh
```

## Production Ops Check

`production-ops-check.sh` runs a minimal production operations check across public endpoints, remote containers, disk usage, Nginx config, MySQL, Redis, latest backup and recent logs.

```bash
bash deploy/scripts/production-ops-check.sh
```

Environment variables:

- `SSH_TARGET`: SSH target, default `root@115.29.229.188`
- `BASE_URL`: public deployment base URL, default `https://yxt.yqej.cn`
- `BACKUP_DIR`: remote backup directory, default `/opt/backups/yanxitong/mysql`
- `MAX_BACKUP_AGE_HOURS`: latest backup age threshold, default `24`
- `DISK_WARN_PERCENT`: disk warning threshold, default `80`
- `DISK_FAIL_PERCENT`: disk failure threshold, default `90`
- `LOG_SINCE`: Docker log lookback window, default `1h`
- `LOG_ERROR_FAIL`: set to `1` to fail on backend ERROR/Exception logs
- `REQUIRE_READINESS_READY`: set to `1` to fail unless `/api/health/readiness` is `READY`

Current staging-before-payment mode should leave `REQUIRE_READINESS_READY=0`; readiness `BLOCKED` is reported as a warning until real WeChat payment is configured.

## Production Security Check

`production-security-check.sh` verifies the current pilot security baseline.

```bash
bash deploy/scripts/production-security-check.sh
```

Environment variables:

- `SSH_TARGET`: SSH target, default `root@115.29.229.188`
- `BASE_URL`: public deployment base URL, default `https://yxt.yqej.cn`
- `BACKUP_ROOT`: remote backup root, default `/opt/backups/yanxitong`
- `EDGE_CONF`: remote edge config path, default `/opt/apps/_edge/conf.d/yanxitong.conf`

Coverage:

- HTTPS security headers
- public 80/443 listeners
- no direct public Yanxitong app/data container ports
- edge rate-limit and security header config tokens
- restricted backup directory permissions
- edge Nginx syntax

## Production Schedule Install

`production-install-schedules.sh` installs server-side cron jobs and helper scripts under `/opt/apps/yanxitong/ops`.

```bash
bash deploy/scripts/production-install-schedules.sh
```

Default cron schedule:

- MySQL backup: daily at `03:10`
- Ops check: every 30 minutes

Environment variables:

- `SSH_TARGET`: SSH target, default `root@115.29.229.188`
- `OPS_DIR`: remote helper script directory, default `/opt/apps/yanxitong/ops`
- `BACKUP_DIR`: remote backup directory, default `/opt/backups/yanxitong/mysql`
- `LOG_DIR`: remote cron log directory, default `/opt/backups/yanxitong/ops-logs`
- `BACKUP_CRON`: cron expression for database backup, default `10 3 * * *`
- `OPS_CHECK_CRON`: cron expression for ops checks, default `*/30 * * * *`
- `BACKUP_RETENTION_DAYS`: default `14`
- `LOG_RETENTION_DAYS`: default `14`

The installer replaces only the crontab block between `# BEGIN YANXITONG OPS` and `# END YANXITONG OPS`, preserving unrelated cron entries.

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
