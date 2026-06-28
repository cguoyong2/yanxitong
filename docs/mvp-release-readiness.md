# MVP Release Readiness

Date: 2026-06-22

## Verdict

The current Yanxitong codebase is closed for local MVP acceptance. It is suitable as the MVP demo and technical acceptance baseline after `bash deploy/scripts/local-acceptance.sh` passes.

It is not yet a production launch baseline. Production launch still requires real payment provider rollout, production secret replacement, public-entry abuse controls and deployment-specific ingress/HTTPS checks.

## Required Verification

Run the full local business acceptance chain:

```bash
bash deploy/scripts/local-acceptance.sh
```

Run the release preflight chain:

```bash
bash deploy/scripts/release-readiness.sh
```

For a production-like environment where `/api/health/readiness` must be strictly ready:

```bash
REQUIRE_READINESS_READY=1 bash deploy/scripts/release-readiness.sh
```

`release-readiness.sh` writes a machine-readable summary to:

```text
${ARTIFACTS_ROOT:-$TMPDIR/yanxitong-release-readiness-<runId>}/summary.json
```

## MVP Scope Matrix

| Area | Status | Evidence |
| --- | --- | --- |
| Engineering skeleton, migrations, MySQL, Redis | Done | `server`, `deploy/docker-compose.yml`, Flyway migrations |
| Tenant context and admin login | Done | `TenantContextFilter`, auth module, admin smoke |
| Configuration center | Done | admin config pages and `/api/admin/*` CRUD endpoints |
| Event type, theme and theme copywriting | Done | admin pages, public meta APIs, smoke theme assertions |
| Banquet creation and theme switching | Done | `/api/banquets`, `banquet.theme_code`, smoke banquet assertions |
| Base invitation instance and public page | Done | invitation module, public invitation smoke |
| Basic invitation editing | Done | `PUT /api/invitations/{id}/basic` |
| Plan rights and entitlement checks | Done | plan order flow, rights APIs, smoke entitlement assertions |
| Lightweight device rental order | Done | `device_config`, `device_order`, admin order status updates |
| Device entity and binding baseline | Done | `device`, `device_bind`, bind by `banquetId` and `bindCode` |
| RSVP | Done | submit/update/list/stats and smoke duplicate-submit check |
| Unified gift payment for online gift and onsite QR | Done | shared `payment_order`, `gift_record`, `favor_entry` path |
| Offline cash gift | Done | writes gift, favor and broadcast logs without payment order |
| Favor ledger and bilateral comparison | Done | manual entries, contacts, compare API |
| Confirm screen | Done | bind, standby, success, offline, latest event, WebSocket |
| Cloud speaker simulation | Done | simulated through `broadcast_log` |
| Operation logs | Done | common `operation_log` for config, business, payment and export actions |
| Payment Provider/Adapter boundary | Done for architecture | mock provider verified locally; real provider remains launch work |
| CSV/XLSX export | Done behind entitlement | `EXCEL_EXPORT`, CSV/XLSX endpoints and smoke workbook checks |

## Confirmed P1 Or Later Scope

| Item | Status |
| --- | --- |
| WeChat service-provider or payment-institution formal rollout | P1 / production launch |
| Complex visual invitation editor | P1 or later |
| Agent, hotel and wedding-company independent workspaces | P1 or later |
| Device inventory scheduling, deposit, repair, return and settlement | P1 or later |
| Real cloud speaker hardware protocol | P1 or later |
| Real hardware SN dependency for confirm-screen binding | Later |
| `favor_compare_snapshot` table | Deferred until performance requires it |
| Separate `confirm_screen_event` table | Deferred until audit/query requirements require it |
| Large export row caps, streaming or async delivery | P1 hardening |

## Release Gates

The MVP baseline can be treated as locally accepted only when all gates pass:

- `mvn -q test` passes in `server`.
- `npm run build` passes in `admin`.
- `npm run build` passes in `confirm-screen`.
- `node deploy/scripts/miniapp-route-check.mjs` passes, unless miniapp checks are explicitly skipped with `SKIP_MINIAPP_BUILD=1`.
- `npm run build` passes in `miniapp`, unless explicitly skipped with `SKIP_MINIAPP_BUILD=1` for a backend/admin-only check.
- `bash deploy/scripts/local-acceptance.sh` passes.
- `/api/health/readiness` has been reviewed. `READY` is required before production, while local demo may show expected default-credential warnings or blockers.

## Production Blockers

Before real launch:

- Set `PAYMENT_MOCK_SUCCESS_ENABLED=false`.
- Replace `admin/admin123`, default DB password, default Redis password and default mock callback secret.
- Configure the real payment provider through the Provider/Adapter layer.
- Validate payment callback signature, amount, order status and provider trade number in a staging or provider sandbox flow.
- Add deployment-level CDN/WAF/Nginx rate limits on top of backend public-entry limits.
- Define callback payload retention, masking and audit policy.
- Add domain, HTTPS and ingress-specific readiness checks once deployment topology is fixed.

## Public Entry Security

The backend now includes a Redis-backed public-entry rate limit baseline for public invitation access, missing-slug enumeration, RSVP submission, gift payment order creation and offline gift entry. See `docs/public-entry-security.md`.

## Production Operations

Use `docs/production-operations-runbook.md` as the operating checklist before real payment launch. It covers environment variable setup, strict readiness, payment provider rollout, payment incident handling and rollback.

## Current Acceptance Commands

```bash
bash deploy/scripts/release-readiness.sh
bash deploy/scripts/local-acceptance.sh
```

The first command checks build/test/readiness posture. The second command checks the full MVP business loop and browser flows.
