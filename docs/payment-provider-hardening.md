# Payment Provider Hardening

This document records the P1 payment boundary after the MVP mock flow.

The detailed P1-A production design is recorded in `docs/p1-payment-production-design.md`.

The real merchant validation checklist is recorded in `docs/wechat-payment-production-checklist.md`.

The launch-readiness review is recorded in `docs/payment-launch-readiness-review.md`.

The production operations checklist is recorded in `docs/production-operations-runbook.md`.

## Goals

- Business services keep using `PaymentService`; they do not call provider APIs directly.
- Provider differences remain behind `PaymentAdapter`.
- Provider configuration is externalized through `payment.*` application properties and environment variables.
- Callback verification happens before business fulfillment.
- Invalid callbacks are recorded instead of being silently dropped.
- Admin can inspect provider configuration status without seeing secrets.

## Configuration

Default local configuration:

```yaml
payment:
  default-provider: MOCK
  providers:
    MOCK:
      enabled: true
      callback-secret: yanxitong-mock-callback-secret
    WECHAT_SERVICE_PROVIDER:
      enabled: false
```

Environment variables:

- `PAYMENT_DEFAULT_PROVIDER`
- `PAYMENT_MOCK_CALLBACK_SECRET`
- `PAYMENT_WECHAT_SP_ENABLED`
- `PAYMENT_WECHAT_MERCHANT_ID`
- `PAYMENT_WECHAT_APP_ID`
- `PAYMENT_WECHAT_SERVICE_PROVIDER_ID`
- `PAYMENT_WECHAT_SUB_MERCHANT_ID`
- `PAYMENT_WECHAT_SUB_APP_ID`
- `PAYMENT_WECHAT_CERT_SERIAL_NO`
- `PAYMENT_WECHAT_CALLBACK_SECRET`
- `PAYMENT_WECHAT_PRIVATE_KEY_PATH`
- `PAYMENT_WECHAT_API_V3_KEY`
- `PAYMENT_WECHAT_NOTIFY_URL`
- `PAYMENT_WECHAT_CERTIFICATE_MODE`
- `PAYMENT_WECHAT_PLATFORM_CERTIFICATE_PATH`
- `PAYMENT_WECHAT_PUBLIC_KEY_ID`
- `PAYMENT_WECHAT_PUBLIC_KEY_PATH`

The production template is `deploy/.env.production.example`. It is a template only; real secrets should live in the deployment secret manager.

## Admin Provider Status

`GET /api/admin/payments/providers`

Returns provider enablement and masked merchant/app/certificate fields. Callback secrets are never returned; the response only exposes whether a secret is configured.

The provider status and launch-readiness endpoints are backed by `PaymentProviderReadinessService`, which is also used by payment order creation for real providers.

## Create-Payment Readiness Gate

`PaymentService` checks the default provider before creating a new external payment order.

- `MOCK` remains available for local acceptance.
- Real providers such as `WECHAT_SERVICE_PROVIDER` must pass provider readiness before adapter `createPayment` is called.
- Missing provider configuration returns `503` with the missing field list.
- No `payment_order` row is inserted when the real provider is not ready.

Required WeChat service-provider fields for create-payment readiness:

- `enabled`
- `merchantId`
- `appId`
- `serviceProviderId`
- `subMerchantId`
- `certificateSerialNo`
- `privateKeyPath`
- `apiV3Key`
- `notifyUrl`
- certificate-mode-specific verification material when applicable

## Callback Verification

Current local verification uses HMAC-SHA256 over the raw callback body and compares it with the submitted `signature` field.

For real WeChat service-provider integration:

- callback envelope and raw callback endpoint shape are prepared
- WeChat Pay Java SDK dependency and SDK config factory are prepared
- service-provider JSAPI prepay creation is implemented behind `WechatPartnerJsapiClient`
- `prepayId` and client `payPayload` are written back to `payment_order`
- official WeChat signature verification and resource decryption are implemented behind `WechatNotificationParserClient`
- callback logs preserve headers, decrypted body, provider event id, event type, resource type and provider serial number when the SDK parser succeeds
- parsed result should continue returning `PaymentCallbackResult`
- business fulfillment should stay in `PaymentCallbackService`

Remaining production validation:

- verify prepay and callback with a real WeChat service-provider merchant/sub-merchant environment
- collect one successful callback sample and one failed verification sample for regression fixtures without storing secrets

## Failure Handling

Callback logs use:

- `verifyStatus=VERIFIED`, `processStatus=SUCCESS`: callback verified and business fulfillment completed
- `verifyStatus=VERIFIED`, `processStatus=FAILED`: callback verified but order lookup, amount check or business fulfillment failed
- `verifyStatus=FAILED`, `processStatus=FAILED`: signature or payload verification failed
- `verifyStatus=VERIFIED`, `processStatus=IGNORED`: callback is not a successful payment event

All failed callback paths remain visible in admin payment callback logs and can be manually resolved.

## Idempotency And Consistency

P1-A6 hardens the paid callback path with both database constraints and service-level checks:

- `payment_order(provider, provider_trade_no)` is unique when the provider trade number is present.
- `gift_record.payment_order_id` is unique when the gift comes from an online payment order.
- `favor_entry.gift_record_id` is unique when the favor entry comes from a gift record.
- A callback with a provider event ID that already has a successful callback log is verified but marked `IGNORED`.
- A paid order that receives the same successful callback again is kept paid, fulfilled idempotently, and the callback is marked `IGNORED`.
- A paid order that receives a different non-empty provider trade number is marked as callback failure and requires manual handling.
- Non-success provider trade states remain `IGNORED` and do not fulfill business records.

## Compensation Operations

P1-A7 adds guarded admin operations for payment incident handling:

- Retry callback: replays a stored callback raw body through the same provider adapter and records a new callback log.
- Compensate fulfillment: re-runs fulfillment for an already paid order; gift and favor writes remain idempotent.

## Admin Payment Operations

The admin payment page is the operational bridge between payment orders, callback exceptions and fulfillment checks.

- Payment orders support page filters for order number, payment status, scene and entry source.
- The route `banquetId` query narrows payment orders and callback records to the current banquet when present.
- The route `orderNo` query pre-fills payment order and callback order filters.
- Payment order summary shows order count, paid count, unpaid count and amount total for the current result set.
- Paid orders can trigger compensate fulfillment; unpaid orders can be manually settled with a provider trade number or manual proof number.
- Payment orders and callback logs can jump to operation logs by `targetType` and `targetId`.
- Callback records support process status, verify status and order number filters, plus a one-click failed-callback view.
- Callback summary shows total callbacks, failed callbacks, resolved callbacks and verify failures for the current result set.
- Broadcast log access remains a separate troubleshooting view because broadcast rows are linked to fulfilled gift records, not directly to payment order IDs.
- Manual settlement: marks an order as paid only when an operator provides a provider transaction number or manual settlement proof number, then runs fulfillment.

Operational boundaries:

- Retry does not mutate the original raw callback body.
- Failed callbacks remain auditable after retry; the original log is marked `HANDLED` with the retry target log id.
- Manual settlement must be used only after external payment confirmation.
- A paid order with a different existing provider transaction number cannot be overwritten by manual settlement.
