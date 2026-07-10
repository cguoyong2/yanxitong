# Production Launch Gap Report

Date: 2026-06-22

Release checked: `v0.2.0-p1-hardening`

GitHub Release: https://github.com/cguoyong2/yanxitong/releases/tag/v0.2.0-p1-hardening

## Current Check

Command:

```bash
SKIP_REMOTE_CHECKS=1 ENV_FILE=deploy/.env.production.example bash deploy/scripts/production-preflight.sh
```

Result:

```text
[production-preflight] ERROR: DB_PASSWORD still contains placeholder value
```

This is expected when checking the tracked example template. The template must be copied into a deployment secret manager or private env file before production preflight can pass.

## Required Secret And Environment Inputs

Replace or provide all of the following outside Git:

- `DB_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `REDIS_PASSWORD`
- `PAYMENT_MOCK_CALLBACK_SECRET`
- `PAYMENT_WECHAT_MERCHANT_ID`
- `PAYMENT_WECHAT_APP_ID`
- `PAYMENT_WECHAT_CERT_SERIAL_NO`
- `PAYMENT_WECHAT_PRIVATE_KEY_PATH`
- `PAYMENT_WECHAT_API_V3_KEY`
- `PAYMENT_WECHAT_NOTIFY_URL`
- `WECHAT_MINIAPP_APP_ID`
- `WECHAT_MINIAPP_APP_SECRET`
- certificate verification material required by `PAYMENT_WECHAT_CERTIFICATE_MODE`

## Required Runtime Gates

Before exposing real payment traffic:

- `APP_ENV=production` or active `prod` profile.
- `PAYMENT_MOCK_SUCCESS_ENABLED=false`.
- `PAYMENT_DEFAULT_PROVIDER=WECHAT_DIRECT`.
- `PAYMENT_WECHAT_DIRECT_ENABLED=true`.
- Admin default password replaced before public/admin exposure.
- `GET /api/health/readiness` returns `READY`.
- `GET /api/admin/payments/launch-readiness` returns `ready=true`.
- Admin payment page shows zero system safety blockers, payment configuration blockers and operational incident blockers.

## Required External Infrastructure

- Public HTTPS domain for admin/backend/confirm-screen.
- Public HTTPS WeChat callback URL routed to `/api/payments/callbacks/wechat-direct`.
- Nginx, CDN or WAF rate limits on public invitation, RSVP, gift payment and offline gift endpoints.
- Redis reachable from backend for public-entry rate limits.
- Database backup and restore procedure.
- Log retention for `payment_order`, `payment_callback_log`, `gift_record`, `favor_entry`, `broadcast_log` and `operation_log`.

## Required WeChat Validation

After direct merchant payment credentials are complete:

1. Confirm payer OpenID belongs to the miniapp AppID bound to the merchant.
2. Confirm miniapp `code2session` can return payer openid through `/api/wechat/miniapp/openid`.
3. Create one isolated low-value gift payment.
4. Create one low-value paid version order and one low-value device order.
5. Confirm `payment_order.prepay_id` and `pay_payload` are stored for each payment.
6. Complete payment and receive successful callbacks.
7. Confirm gift payment creates exactly one `gift_record`, one `favor_entry` and related `broadcast_log`.
8. Confirm version/device callbacks mark their source orders paid.
9. Replay the callback in non-production and confirm duplicate handling becomes `IGNORED`.
10. Capture one successful callback sample and one failed-verification sample after redaction.

## Next Command After Secrets Are Ready

Run static/environment checks first:

```bash
SKIP_REMOTE_CHECKS=1 ENV_FILE=<private-production-env> bash deploy/scripts/production-preflight.sh
```

After deployment is reachable:

```bash
BASE_URL=https://<public-domain> ENV_FILE=<private-production-env> bash deploy/scripts/production-preflight.sh
```
