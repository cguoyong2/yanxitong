# Payment Launch Readiness Review

This review freezes the payment launch-readiness state after P1-A8.

## Current Capability

- Unified payment creation through `PaymentService`.
- Provider-specific logic isolated behind `PaymentAdapter`.
- WeChat service-provider JSAPI prepay through the official WeChat Pay Java SDK.
- WeChat notification signature verification and resource decryption through SDK `NotificationParser`.
- Callback logs retain raw body, headers, decrypted body, provider event id, event type, resource type and provider serial number.
- Duplicate callbacks and duplicate fulfillment are guarded by service checks and database unique constraints.
- Admin can inspect provider status, callback logs, payment orders, retry failed callbacks, compensate paid-order fulfillment and manually settle externally confirmed orders.
- Admin payment page now presents three launch gates: system safety blockers, payment configuration blockers and operational incident blockers.
- Payment launch-readiness is grouped into provider configuration, callback/security material and merchant secret material.
- Failed callback rows show suggested operator actions based on verification status and error category.

## Launch Blockers

The payment flow is not production-ready until all of these are true:

1. `GET /api/admin/payments/launch-readiness` returns `ready=true`.
2. `GET /api/admin/payments/providers` returns `WECHAT_SERVICE_PROVIDER.productionReady=true`.
3. `GET /api/health/readiness` returns `READY` in production-like deployment.
4. The admin payment page shows zero system safety blockers, zero payment configuration blockers and zero failed callback blockers.
5. The public notify URL is reachable by WeChat Pay and points to `/api/payments/callbacks/wechat-service-provider`.
6. At least one real low-value payment has completed successfully.
7. At least one callback replay has been verified in a non-production environment.
8. A failed verification sample has been captured without storing secrets.

## Admin Launch Gates

The payment configuration tab should be used as the first operational view before real payment rollout:

- System safety blockers: sourced from `/api/health/readiness`; includes default passwords, mock switches and provider safety checks.
- Payment configuration blockers: sourced from `/api/admin/payments/launch-readiness`; includes default provider switch, merchant ids, callback URL and key material.
- Operational incident blockers: sourced from failed payment callback logs; these must be resolved or explicitly handled before rollout.

Launch-readiness groups:

- Provider configuration: provider enabled, default provider switched and core merchant fields complete.
- Callback security and notification: notify URL and verification material ready.
- Merchant key material: private key path and API v3 Key ready.

## Merchant-Side Information

Collect these before real integration:

- service provider merchant id
- service provider app id
- sub-merchant id
- sub-merchant app id
- confirmation of whether payer OpenID belongs to service-provider app or sub-merchant app
- merchant certificate serial number
- merchant private key file path or secret mount path
- API v3 key
- certificate mode: `AUTO`, `PLATFORM_CERTIFICATE`, or `PUBLIC_KEY`
- platform certificate path when using `PLATFORM_CERTIFICATE`
- WeChat Pay public key id and public key path when using `PUBLIC_KEY`
- public callback domain and TLS certificate status

## Test Data Isolation

- Use a dedicated test banquet for real merchant validation.
- Use a low-value payment amount.
- Keep test order numbers, callback ids and provider transaction ids in the launch record.
- Do not mix real merchant validation data with demo smoke data.
- Do not delete failed callback logs; mark them `HANDLED` or `IGNORED` with a remark.

## Go-Live Sequence

1. Deploy with WeChat provider configuration present.
2. Keep `PAYMENT_DEFAULT_PROVIDER=MOCK` until provider status is inspected.
3. Confirm `/api/admin/payments/launch-readiness` has no unexpected blocker except default-provider switching.
4. Switch `PAYMENT_DEFAULT_PROVIDER=WECHAT_SERVICE_PROVIDER`.
5. Restart backend services.
6. Confirm launch-readiness is `ready=true`.
7. Create one online gift payment order from the isolated test banquet.
8. Confirm `prepay_id` and `pay_payload` are persisted.
9. Complete payment and wait for callback.
10. Confirm order, gift, favor and broadcast records are created once.
11. Monitor admin payment callbacks for `FAILED` records.

## Monitoring Points

- `payment_order.pay_status`
- `payment_order.provider_trade_no`
- `payment_callback_log.verify_status`
- `payment_callback_log.process_status`
- `payment_callback_log.provider_event_id`
- `gift_record.payment_order_id`
- `favor_entry.gift_record_id`
- `broadcast_log.event_type=GIFT_PAID`
- `operation_log` entries for callback failure, retry, compensation and manual settlement

## Incident Actions

- Use callback retry only after the root cause is fixed.
- Use fulfillment compensation only when the order is already `PAID`.
- Use manual settlement only after external provider confirmation.
- Use `HANDLED` when an incident was externally resolved.
- Use `IGNORED` when the callback or order is confirmed irrelevant.

Admin suggested actions:

- Verification failure: check certificate/public key material, API v3 Key, raw body and callback headers before retry.
- Amount mismatch: reconcile with provider backend; only manually settle after external payment proof.
- Order not found: verify environment and order number; ignore only when confirmed irrelevant.
- Provider trade number mismatch: pause processing and audit before any settlement action.
- Other failed callback: fix the root cause, then retry or mark handled with a remark.

## Rollback

1. Set `PAYMENT_DEFAULT_PROVIDER=MOCK`.
2. Set `PAYMENT_WECHAT_SP_ENABLED=false`.
3. Restart backend services.
4. Run `bash deploy/scripts/local-acceptance.sh`.
5. Keep all payment orders and callback logs for audit.

## Residual Risks

- No real WeChat merchant callback has been verified in this environment yet.
- OpenID ownership must be confirmed with the actual app mode before production traffic.
- Public callback reachability and TLS validity must be tested from outside the local network.
