# Production Operations Runbook

Date: 2026-06-22

## Purpose

This runbook defines the production-preparation workflow for Yanxitong after local MVP acceptance and before real payment launch.

Current boundary:

- The MVP business loop is accepted locally.
- Real WeChat direct merchant credentials and low-value payment validation can happen later.
- Production deployment must still close configuration, readiness and payment incident handling gates before real money collection.

## Required Files

- Production environment template: `deploy/.env.production.example`
- Release readiness gate: `deploy/scripts/release-readiness.sh`
- Full local acceptance: `deploy/scripts/local-acceptance.sh`
- Payment production design: `docs/p1-payment-production-design.md`
- Payment hardening notes: `docs/payment-provider-hardening.md`
- Public entry security: `docs/public-entry-security.md`
- WeChat checklist: `docs/wechat-payment-production-checklist.md`

## Pre-Launch Checklist

1. Copy `deploy/.env.production.example` into the deployment secret manager.
2. Replace all placeholder values.
3. Keep `PAYMENT_MOCK_SUCCESS_ENABLED=false`.
4. Replace default admin password before exposing admin.
5. Use non-default DB and Redis passwords.
6. Set `APP_ENV=production` or activate the `prod` Spring profile.
7. Configure `PAYMENT_DEFAULT_PROVIDER=WECHAT_DIRECT` only after provider credentials are complete.
8. Fill `PAYMENT_WECHAT_NOTIFY_URL` with the public HTTPS callback URL.
9. Mount WeChat private key and certificate/public-key files as secrets, not as committed files.
10. Run strict readiness:

```bash
REQUIRE_READINESS_READY=1 bash deploy/scripts/release-readiness.sh
```

## Fixed Acceptance Commands

Use these three command groups so launch and operations checks stay repeatable.

### Before Deployment

Run from the repository root before uploading a release:

```bash
bash deploy/scripts/production-acceptance-suite.sh
```

This performs the local code/build gate and writes `.artifacts/production-acceptance/<run-id>/summary.json`.

### After Deployment

Run after the public domain, backend and admin frontend are reachable:

```bash
ADMIN_PASSWORD='<admin-password>' \
BASE_URL=https://yxt.yqej.cn \
SKIP_REMOTE_CHECKS=0 \
RUN_PRODUCTION_API=1 \
RUN_PRODUCTION_BROWSER=1 \
RUN_OPS_CHECK=1 \
bash deploy/scripts/production-acceptance-suite.sh
```

Before real WeChat payment launch, keep `REQUIRE_READINESS_READY=0`; readiness may be `BLOCKED` because provider credentials are intentionally incomplete.

For formal real-money launch:

```bash
ADMIN_PASSWORD='<admin-password>' \
BASE_URL=https://yxt.yqej.cn \
SKIP_REMOTE_CHECKS=0 \
REQUIRE_READINESS_READY=1 \
RUN_PRODUCTION_API=1 \
RUN_PRODUCTION_BROWSER=1 \
RUN_OPS_CHECK=1 \
RUN_SECURITY_CHECK=1 \
bash deploy/scripts/production-acceptance-suite.sh
```

### Daily / Manual Patrol

Run manually or through the installed cron helper:

```bash
BASE_URL=https://yxt.yqej.cn bash deploy/scripts/production-ops-check.sh
ssh root@115.29.229.188 '/opt/apps/yanxitong/ops/run-ops-check-cron.sh'
```

Review the generated summary/logs before starting or resuming pilot traffic.

## Public Entry Security

Before pilot traffic:

1. Confirm Redis is available; public rate limits use Redis counters.
2. Confirm Nginx/CDN/WAF has coarse external rate limits.
3. Confirm backend public-entry limits are active for public invitation, RSVP, gift payment order creation and offline gift records.
4. Review `operation_log` for module `SECURITY`, action `PUBLIC_RATE_LIMIT`.
5. Confirm missing public invitation slugs do not expose extra lookup details.

## Readiness Interpretation

`GET /api/health/readiness` returns:

- `READY`: no blockers or warnings. Required before production launch.
- `WARN`: local/demo defaults remain, but no blocker severity exists. This is acceptable only for local demo.
- `BLOCKED`: at least one production blocker exists. Do not launch.

Current readiness blockers include:

- default admin password
- default DB password
- blank Redis password
- default mock callback secret
- mock success endpoint enabled
- default provider still set to `MOCK`
- default payment provider disabled
- default payment provider production config incomplete

## Payment Provider Rollout

When direct merchant credentials are ready:

1. Deploy with `PAYMENT_WECHAT_DIRECT_ENABLED=true`.
2. Set `PAYMENT_DEFAULT_PROVIDER=WECHAT_DIRECT`.
3. Open admin payment configuration and confirm system safety blockers, payment configuration blockers and operational incident blockers are all zero.
4. Confirm admin payment provider status reports no missing items.
5. Confirm `/api/admin/payments/launch-readiness` has no blockers.
6. Run one low-value online gift payment.
7. Verify `payment_order.prepay_id` and client pay payload are stored.
8. Complete payment and verify callback log status is `SUCCESS`.
9. Confirm exactly one `gift_record`, one `favor_entry`, and related `broadcast_log` rows are created.
10. Replay one callback in non-production and verify duplicate callback is marked `IGNORED`.

## Payment Incident Handling

Use the admin payment page:

- Failed callback: filter `/payments?processStatus=FAILED`.
- Verify failure: filter callback verify status `FAILED`.
- Suggested action cards summarize the recommended operator action for failed callback categories.
- Retry callback: use only when the stored raw callback is valid and the underlying issue is fixed.
- Mark handled: use when external reconciliation proves no more system action is required.
- Ignore: use for non-business or known invalid callbacks after review.
- Compensate fulfillment: use for paid orders whose gift/favor/broadcast side effects need an idempotent rerun.
- Manual settlement: use only after external provider proof confirms payment success.

## Payment Query Compensation

Production must enable the payment maintenance task:

```bash
PAYMENT_MAINTENANCE_ENABLED=true
PAYMENT_MAINTENANCE_QUERY_AFTER=PT1M
PAYMENT_MAINTENANCE_PENDING_TIMEOUT=PT30M
PAYMENT_MAINTENANCE_RETRY_DELAY=PT2M
PAYMENT_MAINTENANCE_BATCH_SIZE=50
PAYMENT_MAINTENANCE_FIXED_DELAY_MS=60000
PAYMENT_MAINTENANCE_INITIAL_DELAY_MS=30000
```

The task queries WeChat for pending non-mock orders, fulfills successful payments through the same callback path, closes unpaid orders after the timeout, and retries temporary provider failures. A closed payment releases its client request id so the user can retry without creating duplicate active orders.

An administrator can trigger one batch manually with `POST /api/admin/payments/maintenance/run`. Review `payment_order.provider_status`, `last_queried_at`, `query_attempt_count`, `last_query_error`, `closed_at`, and `close_reason` when diagnosing an order.

Required operator notes:

- Always enter a handling remark.
- Never overwrite a paid order with a conflicting provider transaction number.
- Keep original failed callback rows for audit.

## Rollback

If provider rollout fails before real public launch:

1. Stop public payment entry traffic if already exposed.
2. Set `PAYMENT_DEFAULT_PROVIDER=MOCK`.
3. Set `PAYMENT_WECHAT_SP_ENABLED=false`.
4. Keep `PAYMENT_MOCK_SUCCESS_ENABLED=false` unless running local acceptance.
5. Restart backend services.
6. Preserve payment orders and callback logs for reconciliation.
7. Run:

```bash
bash deploy/scripts/release-readiness.sh
```

For local demo recovery only:

```bash
PAYMENT_MOCK_SUCCESS_ENABLED=true bash deploy/scripts/local-acceptance.sh
```

## Do Not Launch When

- `/api/health/readiness` is not `READY`.
- Admin still uses `admin/admin123`.
- `PAYMENT_MOCK_SUCCESS_ENABLED=true`.
- `PAYMENT_DEFAULT_PROVIDER=MOCK`.
- WeChat notify URL is not public HTTPS.
- Provider callback verification has not been validated with a real or provider-approved sample.
- Public invitation, RSVP and payment-order creation endpoints have no deployment-level rate limiting.
