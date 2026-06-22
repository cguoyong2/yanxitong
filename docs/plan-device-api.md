# Plan Rights And Device API

## Plans

### List active plans

`GET /api/plans`

Returns active plans ordered by `sort_order`.

### Create plan order

`POST /api/plans/orders`

```json
{
  "banquetId": 1,
  "planId": 2
}
```

Behavior:

- Copies price and unit from `plan`.
- Creates `plan_order`.
- Sets `pay_status` to `PAID` for free plans, otherwise `UNPAID`.
- Records an operation log.

### Mock plan payment success

`POST /api/plans/orders/{orderNo}/mock-success`

Available only when `PAYMENT_MOCK_SUCCESS_ENABLED=true`.

MVP uses this to close the version purchase loop in local acceptance. Real provider payment is routed through the shared payment adapter boundary when enabled and configured.
It changes `pay_status` to `PAID`; paid plan orders become the banquet's active entitlement source.

### Banquet entitlements

`GET /api/plans/banquets/{banquetId}/entitlements`

Returns the banquet's current plan, rights and right values. Resolution rules:

- Use the latest paid `plan_order` for the banquet.
- If no paid order exists, fall back to the active free plan, normally `BASIC`.

### Check right

`GET /api/plans/{planId}/rights/check?rightCode=EXCEL_EXPORT`

MVP uses this for reserved rights hints such as Excel export prompts.

`GET /api/plans/banquets/{banquetId}/rights/check?rightCode=DEVICE_RENTAL`

Checks a right against the banquet's active entitlement source.

## Devices

### List enabled device configs

`GET /api/devices/configs`

Returns enabled device configs. Prices, units and delivery methods are maintained by admin configuration.

### Create device order

`POST /api/devices/orders`

```json
{
  "banquetId": 1,
  "deviceType": "CONFIRM_SCREEN",
  "deliveryMethod": "同城配送",
  "rentStartAt": "2026-10-01T12:00:00",
  "rentEndAt": "2026-10-01T22:00:00"
}
```

Behavior:

- Requires the banquet's active plan to include `DEVICE_RENTAL` or the matching device type right such as `CONFIRM_SCREEN`.
- Copies price, unit and delivery method from `device_config`.
- Creates lightweight `device_order`.
- Sets `pay_status` to `UNPAID`.
- Sets `order_status` to `CREATED`.
- Records an operation log.

### List banquet device orders

`GET /api/devices/orders?banquetId=1`

Returns lightweight device orders for the banquet.

### Mock device payment success

`POST /api/devices/orders/{orderNo}/mock-success`

Available only when `PAYMENT_MOCK_SUCCESS_ENABLED=true`.

MVP uses this to close the device order loop in local acceptance. Real provider payment is routed through the shared payment adapter boundary when enabled and configured.
It changes `pay_status` to `PAID` and `order_status` to `CONFIRMED`.

## Admin Orders

- `GET /api/admin/orders/plans`
- `GET /api/admin/orders/devices`
- `POST /api/admin/orders/devices/{orderNo}/status`

Admin order page behavior:

- Version and device orders share banquet ID, payment status and order number filters on the page.
- The route `banquetId` query is used as the default banquet filter when present.
- Version order summary shows order count, paid count, unpaid count and amount total.
- Device order summary shows order count, paid count, fulfillment count and amount total.
- Plan and device orders can jump to the banquet view and operation logs by target type and target ID.
- Device orders keep the MVP lifecycle only: mock payment success, delivering, delivered and cancelled.

Status update request:

```json
{
  "orderStatus": "DELIVERING"
}
```

Allowed `orderStatus` values:

- `CREATED`
- `CONFIRMED`
- `DELIVERING`
- `DELIVERED`
- `CANCELLED`

Rules:

- Paid device orders can move through confirmed, delivering, delivered or cancelled states.
- Unpaid device orders can only be cancelled from the admin endpoint.
- Each status update writes a `DEVICE / UPDATE_ORDER_STATUS` operation log with previous status, new status, order number and pay status.

The admin order page can also call the mock-success endpoints above to handle MVP payment-status testing.

## MVP Boundary

- Real payment provider code exists behind the Payment Provider/Adapter boundary, but formal merchant validation is still required before production use.
- No complex stock scheduling.
- No deposit.
- No repair flow.
- No return flow.
- No hard dependency on real hardware SN.
