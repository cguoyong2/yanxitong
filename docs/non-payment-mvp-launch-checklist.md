# Non-Payment MVP Launch Checklist

Date: 2026-07-07

Domain: `https://yxt.yqej.cn`

## Verdict

The current build can be used for a non-payment MVP pilot after the checks below pass.

This is not a formal real-money launch. Online gift payment, onsite QR payment and paid plan/device payment must remain blocked with a clear "payment not enabled" message until WeChat service-provider or payment-institution production onboarding is complete.

## Allowed Pilot Scope

The following user flows are allowed in the non-payment pilot:

- Miniapp home page and event-type themed pages.
- Banquet creation, banquet detail and banquet management console.
- Theme switching across banquet, favor, invitation and mine pages.
- Basic invitation editing and public invitation sharing path.
- RSVP submission and RSVP statistics.
- Offline cash gift recording.
- Gift record review.
- Favor ledger contact, detail and bilateral comparison.
- Basic plan entitlement and free-version order path.
- Device config visibility and device entitlement blocking when paid rights are missing.
- Confirm-screen bind by `banquetId` and `bindCode`.
- Confirm-screen latest gift event derived from offline gift events.
- Cloud-speaker simulated broadcast logs.
- Admin operation logs, broadcast logs, banquet data and business data review.

## Blocked Before Real Payment Launch

The following flows must not collect real money yet:

- Online gift payment.
- Onsite QR payment.
- Paid plan purchase.
- Paid device rental payment.
- WeChat payment callback processing from a real merchant account.
- Production `REQUIRE_READINESS_READY=1` release gate.

Expected user behavior before real payment launch:

- Payment-oriented pages may show a confirmation UI.
- Clicking payment confirmation must show that payment is not enabled.
- No real provider payment request should be sent.
- Mock-success endpoints must remain disabled in production.

## Required Code Gates

Run these before every pilot upload or server deployment:

```bash
mvn -q test
cd miniapp && npm run check
bash deploy/scripts/release-readiness.sh
```

Run the full local business chain when backend or API flow changes:

```bash
bash deploy/scripts/local-acceptance.sh
```

`local-acceptance.sh` now includes the non-payment MVP API acceptance path. Its consolidated summary must show:

- `backendSmoke.status = passed`
- `nonPaymentFlow.status = passed`
- `publicInvitationSmoke.status = passed`
- `adminSmoke.status = passed`
- `confirmScreenSmoke.status = passed`

For a faster backend-only non-payment check after miniapp interaction changes:

```bash
bash deploy/scripts/non-payment-flow-acceptance.sh
```

## Production Checks Before Pilot

Before sharing the miniapp experience version with test users:

1. Confirm HTTPS and domain access:

   ```bash
   curl -fsS https://yxt.yqej.cn/api/health
   curl -fsS https://yxt.yqej.cn/api/health/readiness
   ```

2. Run the production acceptance suite with remote checks enabled where credentials are available:

   ```bash
   ADMIN_PASSWORD='<admin-password>' \
   BASE_URL=https://yxt.yqej.cn \
   SKIP_REMOTE_CHECKS=0 \
   RUN_PRODUCTION_API=1 \
   RUN_PRODUCTION_BROWSER=1 \
   RUN_OPS_CHECK=1 \
   bash deploy/scripts/production-acceptance-suite.sh
   ```

3. Confirm the readiness interpretation:

   - `READY`: acceptable for broader production launch only after payment credentials are complete.
   - `BLOCKED` caused only by deferred payment provider configuration: acceptable for non-payment pilot.
   - Blockers involving default admin password, default DB/Redis secrets, mock payment enabled, or public ingress failure: not acceptable.

4. Confirm WeChat Mini Program settings:

   - AppID: `wx5cbc30150256d707`
   - Request legal domain: `https://yxt.yqej.cn`
   - API base URL in build: `https://yxt.yqej.cn/api`

5. Generate a fresh preview QR code:

   ```bash
   bash deploy/scripts/miniapp-preview.sh
   ```

## Manual Miniapp Pilot Walkthrough

Use a real phone preview and record screenshots for failed steps:

1. Open the miniapp home page.
2. Switch across banquet types and confirm page theme, color and copy update together.
3. Create one banquet with phone, date, time and address.
4. Confirm required fields block creation when missing.
5. Open banquet detail and confirm status, title, time, location and action buttons.
6. Edit the basic invitation and save it.
7. Preview the public invitation.
8. Submit an RSVP.
9. Open RSVP statistics and verify totals update.
10. Add one offline cash gift.
11. Open gift records and confirm the gift appears.
12. Open favor ledger and confirm the contact/detail/compare views include the gift.
13. Open version/device pages and confirm paid actions stop at "payment not enabled".
14. Open confirm-screen bind page in browser and bind the banquet if needed.
15. Review admin banquet, business, broadcast log and operation log pages.

## Formal Payment Launch Still Requires

Do not remove the payment blocker until all items below are complete:

- WeChat service-provider or payment-institution account approved.
- Sub-merchant material approved.
- Production merchant IDs, app IDs, certificate serial number, private key path and API v3 key configured outside Git.
- Notify URL configured and reachable.
- `GET /api/admin/payments/launch-readiness` returns `ready=true`.
- `GET /api/health/readiness` returns `READY`.
- One isolated low-value payment succeeds.
- Callback creates exactly one `payment_order`, one `gift_record`, one `favor_entry`, confirm-screen event output and broadcast logs.
- Duplicate callback replay remains idempotent.
- Redacted success/failure callback samples are archived.

## Rollback Rule

If pilot users hit payment entry confusion, data write failures, or theme/page navigation regressions:

1. Stop sharing the preview QR code.
2. Keep production payment disabled.
3. Capture screenshot, page path, banquet ID and time.
4. Run `bash deploy/scripts/non-payment-flow-acceptance.sh` against the affected backend.
5. Fix and re-run `npm run check`, `release-readiness.sh` and the manual phone walkthrough before re-sharing.
