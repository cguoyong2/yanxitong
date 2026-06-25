# Production Server Runbook: yxt.yqej.cn

Date: 2026-06-25

Domain: `https://yxt.yqej.cn`

Server IP: `115.29.229.188`

SSH user used during setup: `root`

## Purpose

This runbook records the actual production deployment shape currently used by Yanxitong on `yxt.yqej.cn`. It complements the generic production documents:

- `docs/production-deployment.md`
- `docs/production-operations-runbook.md`
- `docs/production-acceptance-status-2026-06-25.md`

Do not commit production secrets, database passwords, Redis passwords, admin passwords, certificate private keys or payment keys to this repository.

## Current Topology

| Layer | Current value |
| --- | --- |
| Public domain | `yxt.yqej.cn` |
| Public IP | `115.29.229.188` |
| App root | `/opt/apps/yanxitong` |
| Current release symlink | `/opt/apps/yanxitong/current` |
| Current release target | `/opt/apps/yanxitong/releases/20260625174142` |
| Docker network | `yanxitong-net` |
| Edge TLS container | `global-edge-nginx` |
| App web container | `yanxitong-web` |
| Backend container | `yanxitong-server` |
| MySQL container | `yanxitong-mysql` |
| Redis container | `yanxitong-redis` |

Current release directories observed:

```text
/opt/apps/yanxitong/releases/20260624224526
/opt/apps/yanxitong/releases/20260625174142
```

## Container Mounts

`yanxitong-server`:

| Host path | Container path | Mode |
| --- | --- | --- |
| `/opt/apps/yanxitong/releases/20260625174142/server/server.jar` | `/app/server.jar` | read-only |

`yanxitong-web`:

| Host path | Container path | Mode |
| --- | --- | --- |
| `/opt/apps/yanxitong/releases/20260625174142/admin` | `/usr/share/nginx/html/admin` | read-only |
| `/opt/apps/yanxitong/releases/20260625174142/confirm-screen` | `/usr/share/nginx/html/confirm-screen` | read-only |
| `/opt/apps/yanxitong/releases/20260625174142/nginx/default.conf` | `/etc/nginx/conf.d/default.conf` | read-only |

Data containers:

| Container | Volume |
| --- | --- |
| `yanxitong-mysql` | Docker volume `yanxitong-mysql-data` mounted at `/var/lib/mysql` |
| `yanxitong-redis` | Docker volume `yanxitong-redis-data` mounted at `/data` |

## Runtime Routes

`global-edge-nginx` listens on public ports `80` and `443` and terminates HTTPS for `yxt.yqej.cn`.

Expected routing:

| Public path | Upstream |
| --- | --- |
| `/` | `yanxitong-web:80` admin SPA |
| `/confirm-screen/` | `yanxitong-web:80` confirm-screen SPA |
| `/api/` | `yanxitong-server:8080` |
| `/ws/` | `yanxitong-server:8080` |
| `/.well-known/acme-challenge/` | Certbot challenge directory |

`yanxitong-web` static routing:

```nginx
location /confirm-screen/ {
    alias /usr/share/nginx/html/confirm-screen/;
    try_files $uri $uri/ /confirm-screen/index.html;
}

location / {
    root /usr/share/nginx/html/admin;
    try_files $uri $uri/ /index.html;
}
```

The confirm-screen frontend must be built with `base: '/confirm-screen/'` and router history `createWebHistory('/confirm-screen/')`.

## Build Locally

From repository root:

```bash
cd server
mvn clean package -DskipTests

cd ../admin
npm run build

cd ../confirm-screen
npm run build
```

Before deploying a release, run at least:

```bash
bash -n deploy/scripts/production-api-acceptance.sh
bash -n deploy/scripts/production-browser-smoke.sh
node --check deploy/scripts/production-browser-smoke.mjs
```

For broader local confidence:

```bash
bash deploy/scripts/local-acceptance.sh
```

## Deploy A New Release

Use a timestamped release directory. Example:

```bash
RELEASE_ID="$(date +%Y%m%d%H%M%S)"
REMOTE_RELEASE="/opt/apps/yanxitong/releases/${RELEASE_ID}"

ssh root@115.29.229.188 "mkdir -p ${REMOTE_RELEASE}/server ${REMOTE_RELEASE}/admin ${REMOTE_RELEASE}/confirm-screen ${REMOTE_RELEASE}/nginx"
rsync -az server/target/*.jar root@115.29.229.188:${REMOTE_RELEASE}/server/server.jar
rsync -az --delete admin/dist/ root@115.29.229.188:${REMOTE_RELEASE}/admin/
rsync -az --delete confirm-screen/dist/ root@115.29.229.188:${REMOTE_RELEASE}/confirm-screen/
rsync -az deploy/nginx/nginx.production.example.conf root@115.29.229.188:${REMOTE_RELEASE}/nginx/default.conf
```

If the production web Nginx config has server-specific edits, copy it from the current release instead of replacing it with the example:

```bash
ssh root@115.29.229.188 "cp /opt/apps/yanxitong/current/nginx/default.conf ${REMOTE_RELEASE}/nginx/default.conf"
```

Switch the `current` symlink only after files are uploaded:

```bash
ssh root@115.29.229.188 "ln -sfn ${REMOTE_RELEASE} /opt/apps/yanxitong/current"
```

Because the running containers bind to the release target path, switching the symlink alone does not update already-running bind mounts. Recreate or restart containers with the new host paths. If the deployment uses manually-created containers, remove and recreate `yanxitong-server` and `yanxitong-web` using the same environment, network and mounts as the previous deployment.

If only static frontend assets changed within the current release directory, copying files into `/opt/apps/yanxitong/current/admin/` or `/opt/apps/yanxitong/current/confirm-screen/` and restarting `yanxitong-web` is sufficient.

## Static-Only Hotfix

Use this only for admin or confirm-screen static files when backend code and environment do not change.

Confirm-screen example:

```bash
cd confirm-screen
npm run build
cd ..

rsync -az --delete confirm-screen/dist/ root@115.29.229.188:/opt/apps/yanxitong/current/confirm-screen/
ssh root@115.29.229.188 'docker exec yanxitong-web nginx -t && docker restart yanxitong-web'
```

Admin example:

```bash
cd admin
npm run build
cd ..

rsync -az --delete admin/dist/ root@115.29.229.188:/opt/apps/yanxitong/current/admin/
ssh root@115.29.229.188 'docker exec yanxitong-web nginx -t && docker restart yanxitong-web'
```

After static-only hotfixes, also reload the edge container when its config changed:

```bash
ssh root@115.29.229.188 'docker exec global-edge-nginx nginx -t && docker exec global-edge-nginx nginx -s reload'
```

## Service Commands

Check running containers:

```bash
ssh root@115.29.229.188 'docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" | grep -E "yanxitong|global-edge-nginx"'
```

Restart app containers:

```bash
ssh root@115.29.229.188 'docker restart yanxitong-server yanxitong-web'
```

Reload web routing:

```bash
ssh root@115.29.229.188 'docker exec yanxitong-web nginx -t && docker restart yanxitong-web'
ssh root@115.29.229.188 'docker exec global-edge-nginx nginx -t && docker exec global-edge-nginx nginx -s reload'
```

Inspect logs:

```bash
ssh root@115.29.229.188 'docker logs --tail 200 yanxitong-server'
ssh root@115.29.229.188 'docker logs --tail 200 yanxitong-web'
ssh root@115.29.229.188 'docker logs --tail 200 global-edge-nginx'
```

## Verification

Basic checks:

```bash
curl -fsS https://yxt.yqej.cn/api/health
curl -fsSI https://yxt.yqej.cn/login
curl -fsSI https://yxt.yqej.cn/confirm-screen/bind
curl -fsSI https://yxt.yqej.cn/healthz
```

Confirm-screen asset check:

```bash
curl -fsS https://yxt.yqej.cn/confirm-screen/bind | grep '/confirm-screen/assets/'
```

Run repeatable production checks from the local repository:

```bash
ADMIN_PASSWORD='<admin-password>' BASE_URL=https://yxt.yqej.cn bash deploy/scripts/production-api-acceptance.sh
ADMIN_PASSWORD='<admin-password>' BASE_URL=https://yxt.yqej.cn SHARE_SLUG='<share-slug>' bash deploy/scripts/production-browser-smoke.sh
```

The production API acceptance script intentionally does not call mock-success endpoints.

## Operations Check

Run the minimal production operations check after deployments and before manual acceptance:

```bash
bash deploy/scripts/production-ops-check.sh
```

This checks:

- public health, admin login, confirm-screen bind and edge health endpoints
- readiness status
- required Yanxitong containers
- disk usage for `/`, `/opt` and `/var/lib/docker`
- `yanxitong-web` and `global-edge-nginx` config syntax
- MySQL and Redis ping
- latest database backup age and checksum
- recent backend `ERROR`/`Exception` lines
- recent Nginx 5xx responses

Current pre-payment-launch behavior:

- `GET /api/health/readiness` is expected to be `BLOCKED`.
- Leave `REQUIRE_READINESS_READY=0` during technical deployment validation.
- Set `REQUIRE_READINESS_READY=1` only when formal real-payment launch gates must pass.

Verification run on 2026-06-25:

- Failures: `0`
- Warnings: `1`
- Warning reason: readiness `BLOCKED`, expected before real payment launch
- Containers: all Yanxitong containers running
- Disk usage: 17% on checked paths
- Latest backup checksum: passed
- Recent backend errors: 0 in last 1 hour
- Recent Nginx 5xx: 0 in last 1 hour

## Scheduled Operations

Server-side cron is installed for automatic backups and operations checks.

Install or refresh schedules from the repository:

```bash
bash deploy/scripts/production-install-schedules.sh
```

Current installed crontab block:

```cron
# BEGIN YANXITONG OPS
10 3 * * * OPS_DIR=/opt/apps/yanxitong/ops BACKUP_DIR=/opt/backups/yanxitong/mysql LOG_DIR=/opt/backups/yanxitong/ops-logs MYSQL_CONTAINER=yanxitong-mysql DATABASE=yanxitong BACKUP_RETENTION_DAYS=14 LOG_RETENTION_DAYS=14 /opt/apps/yanxitong/ops/run-backup-cron.sh
*/30 * * * * OPS_DIR=/opt/apps/yanxitong/ops BACKUP_DIR=/opt/backups/yanxitong/mysql LOG_DIR=/opt/backups/yanxitong/ops-logs BASE_URL=https://yxt.yqej.cn MAX_BACKUP_AGE_HOURS=26 DISK_WARN_PERCENT=80 DISK_FAIL_PERCENT=90 LOG_SINCE=1h LOG_RETENTION_DAYS=14 /opt/apps/yanxitong/ops/run-ops-check-cron.sh
# END YANXITONG OPS
```

Installed helper scripts:

```text
/opt/apps/yanxitong/ops/backup-mysql.sh
/opt/apps/yanxitong/ops/ops-check.sh
/opt/apps/yanxitong/ops/run-backup-cron.sh
/opt/apps/yanxitong/ops/run-ops-check-cron.sh
```

Cron logs:

```text
/opt/backups/yanxitong/ops-logs/backup-mysql-YYYYMMDD.log
/opt/backups/yanxitong/ops-logs/ops-check-YYYYMMDD.log
```

Manual validation commands:

```bash
ssh root@115.29.229.188 '/opt/apps/yanxitong/ops/run-ops-check-cron.sh'
ssh root@115.29.229.188 '/opt/apps/yanxitong/ops/run-backup-cron.sh'
ssh root@115.29.229.188 'crontab -l | sed -n "/BEGIN YANXITONG OPS/,/END YANXITONG OPS/p"'
```

Validation run on 2026-06-25:

- Cron service: active
- Ops check runner: passed with readiness warning only
- Backup runner: generated `/opt/backups/yanxitong/mysql/yanxitong-20260625182542.sql.gz`
- Log retention: 14 days
- Backup retention: 14 days

## Readiness Gate

Current expected readiness before real WeChat credentials are configured:

```text
GET /api/health/readiness -> BLOCKED
```

This is acceptable for technical deployment validation, but not for formal real-money launch.

Formal production payment launch requires:

- `GET /api/health/readiness` returns `READY`.
- `GET /api/admin/payments/launch-readiness` returns `ready=true`.
- `PAYMENT_MOCK_SUCCESS_ENABLED=false`.
- `PAYMENT_DEFAULT_PROVIDER=WECHAT_SERVICE_PROVIDER`.
- WeChat service-provider/sub-merchant config and callback verification material are complete.
- One isolated low-value payment and callback is verified end to end.

## Rollback

Prefer release-directory rollback, preserving MySQL and Redis volumes.

1. Identify the previous release:

```bash
ssh root@115.29.229.188 'ls -1dt /opt/apps/yanxitong/releases/*'
```

2. Point `current` back to the previous release:

```bash
PREVIOUS_RELEASE="/opt/apps/yanxitong/releases/<previous-release-id>"
ssh root@115.29.229.188 "ln -sfn ${PREVIOUS_RELEASE} /opt/apps/yanxitong/current"
```

3. Recreate or restart `yanxitong-server` and `yanxitong-web` so bind mounts point to the previous release target path. If the current incident is frontend-only and the container already mounts a mutable current release directory, restoring the previous static files and restarting `yanxitong-web` is enough.

4. Validate:

```bash
curl -fsS https://yxt.yqej.cn/api/health
ADMIN_PASSWORD='<admin-password>' BASE_URL=https://yxt.yqej.cn bash deploy/scripts/production-browser-smoke.sh
```

Do not remove or recreate:

- `yanxitong-mysql-data`
- `yanxitong-redis-data`
- `payment_order`
- `payment_callback_log`
- `gift_record`
- `favor_entry`
- `broadcast_log`
- `operation_log`

These are needed for reconciliation and audit.

## Backup Notes

Before formal pilot traffic, add an automated backup policy. The repository now includes repeatable backup and restore scripts.

Create a production backup on the server:

```bash
bash deploy/scripts/production-db-backup.sh
```

Create a backup and also copy it to a local directory:

```bash
LOCAL_COPY_DIR=/tmp/yanxitong-db-backups bash deploy/scripts/production-db-backup.sh
```

Verify a backup by restoring into a temporary database:

```bash
BACKUP_FILE=/opt/backups/yanxitong/mysql/<backup>.sql.gz \
RESTORE_DATABASE=yanxitong_restore_verify_$(date +%Y%m%d%H%M%S) \
DROP_TARGET_FIRST=1 \
bash deploy/scripts/production-db-restore.sh
```

Only restore into the production `yanxitong` database after an explicit rollback decision:

```bash
BACKUP_FILE=/opt/backups/yanxitong/mysql/<backup>.sql.gz \
RESTORE_DATABASE=yanxitong \
DROP_TARGET_FIRST=1 \
CONFIRM_RESTORE=RESTORE_PRODUCTION_YANXITONG \
bash deploy/scripts/production-db-restore.sh
```

Verification run on 2026-06-25:

- Backup generated: `/opt/backups/yanxitong/mysql/yanxitong-20260625181512.sql.gz`
- Checksum verification: passed
- Temporary restore database: restored 30 tables
- Temporary restore database was removed after verification

Verify restore on a separate environment before relying on backups for formal pilot traffic.

## Known Boundaries

- Real WeChat service-provider/sub-merchant production payment is not enabled yet.
- The readiness page is expected to remain `BLOCKED` until real payment provider configuration is complete.
- This server hosts other unrelated containers. Do not stop or prune Docker globally.
- Avoid `docker system prune`, broad volume deletion, or deleting `/opt/apps/yanxitong/releases` without a tested backup and rollback plan.
