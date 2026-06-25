# Production Acceptance Status

Date: 2026-06-25

Domain: `https://yxt.yqej.cn`

Git commit after confirm-screen fix: `2f2194c`

## Current Conclusion

The production deployment is reachable and the MVP operating loop is usable for admin, public invitation APIs, RSVP, offline gift recording, payment-order creation in mock-gated mode, confirm-screen binding pages and operational review pages.

The environment is still intentionally `BLOCKED` for formal production launch because real WeChat service-provider payment is not enabled or fully configured. This is expected until official merchant onboarding, certificates and small-amount payment validation are completed.

## Production Runtime

| Component | Status | Notes |
| --- | --- | --- |
| Public domain | Passed | `https://yxt.yqej.cn` resolves and serves HTTPS traffic. |
| Edge Nginx | Passed | Routes `/`, `/api/`, `/ws/` and `/confirm-screen/`. |
| Admin web | Passed | Login and main admin routes load successfully. |
| Backend API | Passed | `/api/health` is reachable and API flows were verified. |
| MySQL | Passed | Application migrations completed through version 21. |
| Redis | Passed | Application starts with Redis configured. |
| Confirm-screen static app | Passed | Served under `/confirm-screen/` with correct asset paths. |
| Real payment launch | Blocked | Waiting for WeChat service-provider/sub-merchant production configuration and validation. |

## Browser Walkthrough

Browser walkthrough result:

| Check | Result | Notes |
| --- | --- | --- |
| Admin login | Passed | Redirected to `/dashboard`. |
| Dashboard | Passed | Shows production readiness status. |
| Config center | Passed | Admin page reachable. |
| Event types | Passed | Admin page reachable. |
| Theme config | Passed | Admin page reachable. |
| Theme copywriting | Passed | Admin page reachable. |
| Plans and rights | Passed | Admin page reachable. |
| Template preview | Passed | Admin page reachable. Page title is `模板预览`. |
| Device config | Passed | Admin page reachable. |
| Banquet management | Passed | Admin page reachable. |
| Business data | Passed | Admin page reachable. |
| Payment management | Passed | Admin page reachable. |
| Broadcast logs | Passed | Admin page reachable. |
| Operation logs | Passed | Admin page reachable. |
| Confirm-screen bind page | Passed | `https://yxt.yqej.cn/confirm-screen/bind` renders binding UI. |
| Public invitation API | Passed | Public invitation endpoint returns the acceptance invitation data. |

Browser runtime issues observed:

- Console errors: `0`
- Request failures: `0`

## API Acceptance Sample

Acceptance data generated during production verification:

| Item | Value |
| --- | --- |
| Banquet ID | `1` |
| Invitation ID | `1` |
| Share slug | `9d0a4fac4ac8405b` |
| Public invitation API | `https://yxt.yqej.cn/api/invitations/public/9d0a4fac4ac8405b` |
| Confirm-screen bind code | `PROD-CS-prod20260625174952` |
| Payment order | `GP202606250950378977` |

Verified flows:

- Admin login token creation.
- Event type, theme and config metadata retrieval.
- Banquet creation and invitation creation.
- Public invitation API retrieval.
- RSVP submission.
- Offline gift recording.
- Gift summary retrieval.
- Confirm-screen bind/status retrieval.
- Payment order creation through the existing provider boundary.

## Current Readiness Gate

`GET /api/health/readiness` currently returns:

```json
{
  "status": "BLOCKED",
  "blockers": [
    "默认支付通道不是 MOCK",
    "默认支付通道生产配置完整"
  ],
  "warnings": []
}
```

`GET /api/admin/payments/launch-readiness` currently returns `ready=false`.

Current payment blockers:

- `支付通道已启用`
- `默认通道已切换`
- `核心商户配置完整`

Ready payment groups:

- Callback security material
- Merchant secret material

Interpretation:

- The application is correctly preventing formal production payment launch.
- The remaining blocker is external production payment onboarding/configuration, not core MVP application availability.

## Completed MVP Modules

| Module | Status | Notes |
| --- | --- | --- |
| Engineering skeleton and deployment loop | Completed | Backend, admin, confirm-screen, DB, Redis and Nginx loop are in place. |
| Login and admin authentication | Completed | Admin login works in production. |
| Configuration center | Completed | Config pages and operational config APIs are available. |
| Banquet creation and scene theme | Completed | Banquet, event type and theme fields are wired. |
| Basic invitation/public entry | Completed | Public invitation API and invitation linkage are verified. |
| Template basics | Completed | Template preview/list capability is available; complex editor remains out of MVP. |
| Version rights | Completed | Plans and entitlement gates exist. |
| Device rental basics | Completed | Device config/order foundation exists; complex inventory lifecycle is out of MVP. |
| RSVP | Completed | RSVP submission and admin review loop are verified. |
| Gift payment order boundary | Completed | Payment order creation uses provider boundary; real provider launch is pending. |
| Offline cash gift | Completed | Offline gift recording works. |
| Gift record and favor ledger | Completed | Gift/favor write path is present. |
| Confirm-screen | Completed with fix | Production subpath asset routing fixed in commit `2f2194c`. |
| Cloud-speaker simulation logs | Completed | Broadcast log page is available. |
| Operation logs | Completed | Operation log page is available. |
| Export entitlement foundation | Completed | Export endpoints and entitlement checks are documented; larger-scale hardening remains P1. |

## Remaining Work Before Formal Production Launch

1. Complete WeChat service-provider and sub-merchant onboarding.
2. Configure production payment provider values outside Git:
   - service-provider merchant ID
   - sub-merchant ID
   - app ID / sub-app ID when needed
   - certificate serial number
   - private key path
   - API v3 Key
   - notify URL
3. Run production preflight after real payment secrets are mounted.
4. Perform one isolated low-value payment verification.
5. Confirm successful callback creates exactly one gift record, favor entry and broadcast log.
6. Archive redacted successful and failed callback samples.
7. Add database backup, log retention and monitoring policies for ongoing operation.

## Next Engineering Recommendation

Continue with a production hardening pass that does not require WeChat credentials:

1. Document the current deployment commands and rollback steps for the actual server path.
2. Add scheduled or manual runbook steps for the repeatable production smoke scripts.
3. Keep real payment provider enablement as a separate gated launch task.

## Repeatable Production Checks

Two repeatable production checks have been added:

```bash
ADMIN_PASSWORD='<admin-password>' BASE_URL=https://yxt.yqej.cn bash deploy/scripts/production-api-acceptance.sh
ADMIN_PASSWORD='<admin-password>' BASE_URL=https://yxt.yqej.cn SHARE_SLUG='<share-slug>' bash deploy/scripts/production-browser-smoke.sh
```

Validation run on 2026-06-25:

| Script | Result | Evidence |
| --- | --- | --- |
| `production-api-acceptance.sh` | Passed | Created banquet `2`, invitation `2`, share slug `6e430af01957488d`, bind code `PROD-CS-20260625180503`, payment order `GP202606251005075449`. |
| `production-browser-smoke.sh` | Passed | Admin pages, confirm-screen bind page and public invitation API all loaded with zero runtime failures. |

The API acceptance script does not call mock-success endpoints. It creates a payment order when the current provider allows order creation, but does not simulate payment completion.

The actual server deployment and rollback steps are recorded in `docs/production-server-runbook-yxt-yqej-cn.md`.

Database backup and restore scripts have also been added:

- `deploy/scripts/production-db-backup.sh`
- `deploy/scripts/production-db-restore.sh`

Validation run on 2026-06-25:

- Backup generated at `/opt/backups/yanxitong/mysql/yanxitong-20260625181512.sql.gz`
- SHA-256 verification passed
- Restore verification recovered 30 tables into a temporary database
- Temporary verification database was removed after the test

Minimal production operations check has been added:

- `deploy/scripts/production-ops-check.sh`

Validation run on 2026-06-25:

- Failures: `0`
- Warnings: `1`
- Warning reason: readiness `BLOCKED`, expected before real payment launch
- Containers, disk, Nginx config, MySQL, Redis, latest backup checksum and recent logs passed

Automatic server-side schedules have been installed:

- MySQL backup daily at `03:10`
- Operations check every 30 minutes
- Helper scripts installed under `/opt/apps/yanxitong/ops`
- Logs stored under `/opt/backups/yanxitong/ops-logs`
- Backup and log retention: 14 days
- Manual validation generated `/opt/backups/yanxitong/mysql/yanxitong-20260625182542.sql.gz`

Pilot security hardening has been applied:

- Edge security headers enabled, including HSTS and CSP
- Edge rate limits enabled for public invitation, RSVP, gift order, offline gift and login
- Backup directories restricted to `700 root:root`
- Yanxitong containers are not directly published to public host ports
- `production-security-check.sh` passed with one unrelated shared-server port warning

Details are recorded in `docs/production-security-hardening-2026-06-25.md`.

Miniapp production preparation has been added:

- `miniapp/src/api/client.ts` now reads `VITE_API_BASE_URL`.
- `miniapp/.env.example` documents the configurable API base URL.
- Local production build verified `miniapp/dist/build/mp-weixin` contains `https://yxt.yqej.cn/api`.
- Deployment notes are recorded in `docs/miniapp-production-deployment.md`.
- Miniapp AppID `wx5cbc30150256d707` has been configured in `miniapp/src/manifest.json`.
- WeChat request legal domain `https://yxt.yqej.cn` has been configured in the WeChat console by the owner.
- Formal public release remains blocked until real WeChat payment is enabled.
