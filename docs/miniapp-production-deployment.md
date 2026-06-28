# Miniapp Production Deployment

Date: 2026-06-25

## Current Conclusion

The WeChat miniapp can be built and imported into WeChat DevTools for preview or experience-version testing after filling the real AppID.

Formal public release should wait until the real WeChat service-provider/sub-merchant payment flow is complete. Current production backend can create payment orders, but real payment completion is intentionally blocked until WeChat payment credentials and callback verification are ready.

## Build

Production API base URL is configured by `miniapp/.env.production`:

```text
VITE_API_BASE_URL=https://yxt.yqej.cn/api
```

Create it from the tracked example when needed:

```bash
cp miniapp/.env.example miniapp/.env.production
```

Then keep only the production line in `miniapp/.env.production`:

```text
VITE_API_BASE_URL=https://yxt.yqej.cn/api
```

Build command:

```bash
cd miniapp
npm run build
```

Build output:

```text
miniapp/dist/build/mp-weixin
```

Before preview or upload, run the miniapp route registration check from the repository root:

```bash
node deploy/scripts/miniapp-route-check.mjs
```

This fails when a static miniapp navigation target is missing from `miniapp/src/pages.json`.

Import this directory into WeChat DevTools.

## Required WeChat Console Settings

Before preview, upload or experience-version testing:

1. Confirm the miniapp AppID in `miniapp/src/manifest.json`: `wx5cbc30150256d707`.
2. In WeChat Mini Program console, add request legal domain:

```text
https://yxt.yqej.cn
```

Current status: configured by owner.

3. Confirm the server TLS certificate is valid for `yxt.yqej.cn`.
4. Do not enable "ignore domain verification" for production or review builds.

## Current Production Boundaries

Available for preview and technical experience testing:

- Banquet creation.
- Invitation template selection.
- Basic invitation editing.
- Public invitation rendering.
- RSVP submission.
- Offline gift recording.
- Gift/favor list and summary pages.
- Device and plan pages for order creation/status review.

Not ready for formal public release:

- Real WeChat payment completion.
- Service-provider/sub-merchant payment callback verification.
- Miniapp `wx.requestPayment` integration using real `payment_order.pay_payload`.

## Mock Payment Behavior

Mock-success buttons are controlled by backend runtime feature `mockPaymentEnabled`.

Production deployment has:

```text
PAYMENT_MOCK_SUCCESS_ENABLED=false
```

Therefore miniapp mock-success buttons are hidden in production. Users may create pending orders, but payment completion requires the real WeChat payment rollout.

## Deployment Checklist

Before experience-version testing:

- `cd miniapp && npm run build` passes.
- `node deploy/scripts/miniapp-route-check.mjs` passes.
- `miniapp/src/manifest.json` contains the real AppID `wx5cbc30150256d707`.
- WeChat request legal domain includes `https://yxt.yqej.cn`.
- Complete `docs/miniapp-experience-regression-checklist.md` for the generated preview or experience version.
- `bash deploy/scripts/production-api-acceptance.sh` passes.
- `bash deploy/scripts/production-browser-smoke.sh` passes.
- `bash deploy/scripts/production-security-check.sh` passes.

Before formal public release:

- `GET /api/health/readiness` returns `READY`.
- `GET /api/admin/payments/launch-readiness` returns `ready=true`.
- WeChat service-provider/sub-merchant credentials are configured.
- One isolated low-value miniapp payment passes end to end.
- Successful callback creates exactly one `gift_record`, one `favor_entry` and related `broadcast_log`.
