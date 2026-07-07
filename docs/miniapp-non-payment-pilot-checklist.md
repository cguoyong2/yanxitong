# Miniapp Non-Payment Pilot Checklist

This checklist is for the first WeChat Mini Program experience-version test before real WeChat payment is enabled.

For the full code, production and payment-boundary checklist, use `docs/non-payment-mvp-launch-checklist.md`.

## Build Artifact

Import this directory in WeChat DevTools:

```text
miniapp/dist/build/mp-weixin
```

Expected production build settings:

- AppID: `wx5cbc30150256d707`
- API base URL: `https://yxt.yqej.cn/api`
- WeChat request legal domain: `https://yxt.yqej.cn`

## Test Scope

Run these flows first:

1. Open the miniapp home page.
2. Create a banquet.
3. Confirm the banquet detail page shows event type, theme, location and invitation share path.
4. Open the public invitation page.
5. Edit basic invitation fields.
6. Submit an RSVP from the invitation page.
7. Open RSVP stats from the banquet detail page.
8. Add an offline cash gift.
9. Open gift records and confirm the offline gift appears.
10. Open the favor ledger and confirm the gift can be reviewed from ledger context.
11. Open confirm-screen bind page from the web deployment and bind with a banquet ID and bind code when needed.
12. Review admin pages for banquets, business data, broadcast logs and operation logs.

Before sharing the preview QR code, the consolidated local acceptance summary should show `nonPaymentFlow.status = passed`.

## Payment Entry Expectation

Before real payment launch:

- Online gift and onsite QR payment entries should not be available in production miniapp pages.
- Direct access to the gift payment page should show that online gift and onsite QR are not open yet.
- Mock payment-success buttons remain hidden in production.

Do not run real online gift payment or onsite QR payment tests until the WeChat service-provider or payment-institution provider is configured and a low-value payment callback has been validated.

Paid plan and paid device flows should show payment confirmation UI but must stop with a clear "payment not enabled" message before real payment provider launch.

## Issue Recording

For each issue found during experience-version testing, record:

- Device model and WeChat version.
- Page path.
- Screenshot or screen recording.
- Operation steps.
- Expected result.
- Actual result.
