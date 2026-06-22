# MVP Closure Review

Date: 2026-06-22

## Review Verdict

The current codebase has passed local MVP acceptance and can be treated as a local demo-accepted baseline. It is not production-launch ready yet because payment provider rollout, mock endpoint gating, secret management and deployment-level public-entry protection still need hardening.

Latest acceptance evidence:

- Summary: `/var/folders/4b/gwp_mz5x1sb7_9rq71ydgnpw0000gn/T/yanxitong-local-acceptance-20260622212002/summary.json`
- Result: backend smoke, public invitation smoke, admin smoke and confirm-screen smoke all passed.
- Visual coverage: public invitation 2 screenshots, admin 14 screenshots, confirm screen 8 screenshots.

Latest release-readiness preflight:

- Summary: `/var/folders/4b/gwp_mz5x1sb7_9rq71ydgnpw0000gn/T/yanxitong-release-readiness-20260622211854/summary.json`
- Result: backend tests, admin build, confirm-screen build and miniapp build all passed.
- Readiness endpoint: not checked in this run because backend was not already reachable before preflight; strict production checks can use `REQUIRE_READINESS_READY=1 bash deploy/scripts/release-readiness.sh`.

## Scope Status

Implemented MVP baseline:

- Engineering skeleton, Flyway migrations, MySQL, Redis, tenant context, login auth and admin config center.
- Event types, themes, theme copywriting, plans, templates and device config maintained from backend/admin.
- Banquet creation, event-type theme switching, banquet detail, base invitation instance, public invitation sharing and basic invitation editing.
- Plan purchase, entitlement activation, backend rights checks and order idempotency.
- Lightweight device rental loop: need-device choice, rental time, price/unit, delivery method, payment status and admin review.
- RSVP, unified online gift payment entry, onsite QR payment entry, offline cash gift entry, gift records and favor ledger writes.
- Confirm-screen binding, standby, latest gift success display, offline state, WebSocket delivery and broadcast logs.
- Cloud-speaker simulation through broadcast logs.
- General operation logs for configuration, business and key action paths.
- Admin business review, banquet aggregate detail, orders, payments, broadcast logs, operation logs and CSV/XLSX export under `EXCEL_EXPORT`.

## Database Review

Required structures are present:

- `banquet.theme_code`
- `theme_copywriting`
- `device_config`, `device_order`, `device`, `device_bind`
- `payment_order`, `gift_record`, `favor_entry`, `broadcast_log`
- `operation_log`

Intentionally deferred structures remain absent:

- `favor_compare_snapshot`
- `confirm_screen_event`

## Architecture Review

The payment path follows the confirmed rule that online gift and onsite QR share one payment capability. Both paths converge into `payment_order`, `gift_record` and `favor_entry`, then reuse callback handling, confirm-screen push and cloud-speaker simulation logs.

Payment provider integration is behind the Provider/Adapter boundary. Current local flows use mock callbacks for verification; WeChat service-provider and sub-merchant rollout should remain a production-readiness task.

Invitation capability is separated into the invitation module instead of being scattered entirely in config. Config still owns template/config maintenance, while invitation owns banquet invitation instances, public access and sharing behavior.

Theme copywriting fallback order matches the confirmed rule:

1. Banquet custom copywriting.
2. Theme copywriting configuration.
3. Event-type default copywriting.
4. System default copywriting.

## Found Conflicts

Resolved in this review:

- Export scope wording was stale in several docs. Runtime now provides CSV and native XLSX export endpoints/buttons behind `EXCEL_EXPORT`, while some documents still said native XLSX was later work.
- The current codebase includes completed P1 export capability. Docs should describe this as "current baseline includes CSV/XLSX", while preserving that original MVP did not require full formal export.

Still worth tracking:

- Some historical initialization/review docs say formal Excel export was not implemented at that earlier point. These should be read as point-in-time records, not current scope statements.

## Production Blockers

Must be handled before real production launch:

- Keep mock success endpoints and mock payment buttons gated by `PAYMENT_MOCK_SUCCESS_ENABLED`; production deployments must leave it disabled.
- Replace default admin credentials and local default secrets with managed production secrets.
- Complete real WeChat service-provider or payment-institution provider configuration and low-value payment validation.
- Define payment callback body retention, redaction and audit policy.
- Keep backend public-entry rate limits enabled and add deployment-level CDN/WAF/Nginx protection for public invitation, RSVP and payment-order creation endpoints.

## Pilot Hardening

Recommended before a broader user pilot:

- Add focused tests for favor contact filtering by `banquetId`.
- Add tests for payment duplicate handling and callback idempotency.
- Add pagination or backend limits for admin gift, RSVP, order, payment, broadcast and operation-log lists.
- Expand banquet aggregate logs beyond direct `targetType=banquet` records if operators need a complete event timeline.
- Monitor favor contact aggregation performance before deciding whether `favor_compare_snapshot` is needed.

## Deferred Scope

The following remain correctly outside the current MVP baseline:

- Complex visual invitation editor.
- Agent, hotel and wedding-company independent workspaces.
- Device inventory scheduling, deposit, repair, return and settlement workflows.
- Real hardware SN dependency for confirm-screen binding.
- `favor_compare_snapshot`, unless ledger comparison performance requires it.
- Separate `confirm_screen_event`, unless event audit or query performance requires it.

## Next Recommended Tasks

1. Prepare WeChat service-provider rollout configuration as a disabled provider profile for later formal launch.
2. Add deployment documentation that explicitly keeps `PAYMENT_MOCK_SUCCESS_ENABLED=false` outside demo and local acceptance environments.
3. Extend `/api/health/readiness` with deployment-specific domain, HTTPS and ingress checks when production ingress is known.
4. Add visible admin dashboard warning if `/api/health/readiness` is not `READY`.
5. Add alerting around failed payment callbacks and readiness blockers.
