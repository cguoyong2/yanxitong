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

1. Add a repeatable production browser smoke script for admin and confirm-screen.
2. Add a production acceptance script that verifies the already-tested API path without exposing secrets.
3. Document the current deployment commands and rollback steps for the actual server path.
4. Keep real payment provider enablement as a separate gated launch task.
