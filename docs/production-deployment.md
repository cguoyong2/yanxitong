# Production Deployment Minimum Loop

Date: 2026-06-22

## Purpose

This document defines the minimum deployable production loop for Yanxitong. It is an example deployment path, not a replacement for managed cloud services, secrets management, TLS termination or provider-side payment validation.

## Files

- `deploy/.env.production.example`: production environment variable template.
- `deploy/docker-compose.production.yml`: production-style Compose example.
- `server/Dockerfile`: backend image build.
- `deploy/nginx/nginx.production.example.conf`: Nginx reverse proxy/static hosting example.
- `deploy/scripts/production-preflight.sh`: environment and deployment readiness check.

## Build

Backend image is built by Compose from `server/Dockerfile`.

Build frontend static assets before starting Nginx:

```bash
cd admin
npm run build

cd ../confirm-screen
npm run build
```

Miniapp remains a WeChat mini-program build artifact and is not served by Nginx in this minimum loop:

```bash
cd miniapp
npm run build
```

## Configure

Create the real environment file from the template:

```bash
cp deploy/.env.production.example deploy/.env.production
```

Replace every placeholder value. Real deployments should put secrets in the platform secret manager instead of committing `.env.production`.

Required production safety values:

- `APP_ENV=production`
- `PAYMENT_MOCK_SUCCESS_ENABLED=false`
- non-default `DB_PASSWORD`
- non-empty `REDIS_PASSWORD`
- non-default `PAYMENT_MOCK_CALLBACK_SECRET`

When real payment is enabled:

- `PAYMENT_DEFAULT_PROVIDER=WECHAT_SERVICE_PROVIDER`
- `PAYMENT_WECHAT_SP_ENABLED=true`
- `PAYMENT_WECHAT_NOTIFY_URL=https://<public-domain>/api/payments/callbacks/wechat-service-provider`
- WeChat private key/certificate/public-key files are mounted as secrets.

## Preflight

Run static/environment preflight:

```bash
SKIP_REMOTE_CHECKS=1 ENV_FILE=deploy/.env.production bash deploy/scripts/production-preflight.sh
```

After services are reachable:

```bash
BASE_URL=https://<public-domain> ENV_FILE=deploy/.env.production bash deploy/scripts/production-preflight.sh
```

This checks environment variables, mock payment gates, frontend build artifacts, `/api/health` and `/api/health/readiness`.

## Start

Example Compose startup:

```bash
docker compose --env-file deploy/.env.production -f deploy/docker-compose.production.yml up -d --build
```

If the env file is not `deploy/.env.production`, pass the same file to Compose and the server `env_file` reference:

```bash
YANXITONG_ENV_FILE=/absolute/path/.env.production \
docker compose --env-file /absolute/path/.env.production -f deploy/docker-compose.production.yml up -d --build
```

Nginx routes:

- `/api/*` to backend
- `/ws/*` to backend WebSocket
- `/` to admin static assets
- `/confirm-screen/*` to confirm-screen static assets

The Nginx example also includes coarse public-entry rate limits for public invitations, RSVP, gift payment order creation and offline gift records. Backend Redis-backed limits remain active separately.

## Verify

Run:

```bash
curl -fsS https://<public-domain>/api/health
curl -fsS https://<public-domain>/api/health/readiness
BASE_URL=https://<public-domain> ENV_FILE=deploy/.env.production bash deploy/scripts/production-preflight.sh
```

For local product acceptance, continue using:

```bash
bash deploy/scripts/local-acceptance.sh
```

Do not use local acceptance as proof of real payment readiness. Real payment launch still requires the payment rollout checks in `docs/production-operations-runbook.md`.

## Rollback

1. Stop public traffic at the load balancer or Nginx.
2. Keep database and Redis volumes intact.
3. Restore the previous backend image or Compose file.
4. Restart services.
5. Preserve payment orders, callback logs and operation logs for reconciliation.
6. Re-run production preflight.

## Boundaries

Still required outside this minimum loop:

- TLS certificate and domain management.
- Secret-manager integration.
- Database backup/restore policy.
- Log shipping and metrics.
- CDN/WAF policy.
- Real WeChat service-provider validation.
