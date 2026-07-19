# WeChat Payment Production Checklist

This checklist is for the first real WeChat direct-merchant payment validation.

## Configuration

Set these variables before switching production traffic to real payment:

- `PAYMENT_DEFAULT_PROVIDER=WECHAT_DIRECT`
- `PAYMENT_WECHAT_DIRECT_ENABLED=true`
- `PAYMENT_WECHAT_MERCHANT_ID`
- `PAYMENT_WECHAT_APP_ID`
- `PAYMENT_WECHAT_CERT_SERIAL_NO`
- `PAYMENT_WECHAT_PRIVATE_KEY_PATH`
- `PAYMENT_WECHAT_API_V3_KEY`
- `PAYMENT_WECHAT_NOTIFY_URL=https://<public-domain>/api/payments/callbacks/wechat-direct`
- `WECHAT_MINIAPP_APP_ID`
- `WECHAT_MINIAPP_APP_SECRET`
- `PAYMENT_WECHAT_CERTIFICATE_MODE`
- `PAYMENT_WECHAT_PLATFORM_CERTIFICATE_PATH` when `PLATFORM_CERTIFICATE`
- `PAYMENT_WECHAT_PUBLIC_KEY_ID` and `PAYMENT_WECHAT_PUBLIC_KEY_PATH` when `PUBLIC_KEY`

Do not configure private keys, API v3 keys, or platform certificates through admin UI.

## Backend Checks

1. Start the backend with `WECHAT_DIRECT` enabled.
2. Open `GET /api/admin/payments/providers`.
3. Confirm `WECHAT_DIRECT.productionReady=true`.
4. Confirm masked merchant identifiers are correct.
5. Confirm `missingItems=[]`.
6. Confirm the notify URL is reachable from the public internet and maps to `/api/payments/callbacks/wechat-direct`.

## Prepay Checks

1. Confirm `POST /api/wechat/miniapp/login` succeeds and the returned miniapp session contains the payer OpenID.
2. Create a gift payment order from the online gift entry.
3. Confirm `payment_order.provider=WECHAT_DIRECT`.
4. Confirm `payment_order.prepay_id` is not empty.
5. Confirm `payment_order.pay_payload` contains `appId`, `timeStamp`, `nonceStr`, `package`, `signType`, and `paySign`.
6. Create one paid version order and one paid device order; confirm both create `payment_order` rows with `biz_order_type` and `biz_order_no`.
7. Confirm no business service calls WeChat SDK directly; payment creation must go through `PaymentService` and `PaymentAdapter`.

## Callback Checks

1. Complete one real successful payment.
2. Confirm one `payment_callback_log` row is created with `verify_status=VERIFIED` and `process_status=SUCCESS`.
3. Confirm `payment_order.pay_status=PAID`.
4. Confirm `payment_order.provider_trade_no` stores the final WeChat transaction ID.
5. Confirm gift payment creates exactly one `gift_record` and one `favor_entry`.
6. Confirm version payment marks the matching `plan_order.pay_status=PAID`.
7. Confirm device payment marks the matching `device_order.pay_status=PAID` and `device_order.order_status=CONFIRMED`.
8. Confirm confirm-screen and cloud-speaker broadcast logs are generated from the same paid gift event path.
9. Replay the same successful callback once in a non-production environment.
10. Confirm the replayed callback is marked `IGNORED` and does not create duplicate fulfillment rows.
11. Create a redacted callback fixture from the successful sample following `docs/wechat-callback-fixture-policy.md`.

## Failure Checks

1. Send a callback with an invalid signature in a non-production environment.
2. Confirm `payment_callback_log.verify_status=FAILED`.
3. Confirm the raw body and headers are retained.
4. Confirm no gift record or favor entry is created.
5. Open admin payment management and filter failed callbacks.
6. Add a handling remark and mark the callback as `HANDLED` or `IGNORED`.
7. Confirm the operation is recorded in `operation_log`.
8. Use callback retry only after fixing the root cause, then confirm a new callback log is created.
9. Create a redacted failed-verification fixture following `docs/wechat-callback-fixture-policy.md`.

## Idempotency Checks

1. Confirm `gift_record.payment_order_id` has only one row for the paid order.
2. Confirm `favor_entry.gift_record_id` has only one row for the created gift record.
3. Confirm a paid order cannot be overwritten by a different provider transaction ID.
4. Confirm duplicate provider events remain visible in callback logs for audit.

## Compensation Checks

1. For a paid order with missing fulfillment records, use `补履约`.
2. Confirm the operation does not create duplicate `gift_record` or `favor_entry` rows.
3. For an externally confirmed paid order that missed callback processing, use `人工核销`.
4. Confirm the operator enters a provider transaction number or manual settlement proof number.
5. Confirm the order becomes `PAID` and fulfillment runs once.

## Rollback

If WeChat validation fails before production launch:

1. Set `PAYMENT_DEFAULT_PROVIDER=MOCK`.
2. Set `PAYMENT_WECHAT_DIRECT_ENABLED=false`.
3. Restart backend services.
4. Confirm local smoke tests still pass.
5. Keep failed callback logs for diagnosis; do not delete them.
