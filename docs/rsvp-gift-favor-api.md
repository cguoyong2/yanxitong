# RSVP, Gift And Favor API

## RSVP

- `POST /api/rsvp/submit`
- `GET /api/rsvp/list?banquetId=1`
- `GET /api/rsvp/stats?banquetId=1`

Submit behavior:

- `attendanceStatus` supports `ATTEND`, `ATTENDING`, `PENDING` and `DECLINED`.
- If `phone` is present, repeated submissions for the same banquet and phone update the existing record.
- If `phone` is absent, repeated submissions match by banquet, guest name and invitation ID when available.
- The returned RSVP record includes transient field `created`; `true` means first submit, `false` means an existing RSVP was updated.
- The miniapp RSVP page validates name, phone format and guest count before submit.
- After submit, the miniapp shows a result state with actions to go to gift payment, return to the invitation, or edit again.

Stats include total records, attending records, pending records, declined records, total guests, meal-required guests and accommodation-required guests.

## Unified Online Gift Payment

Online gift and onsite QR payment share the same API:

`POST /api/gifts/payment-orders`

```json
{
  "banquetId": 1,
  "entrySource": "ONLINE_GIFT",
  "guestName": "张三",
  "amount": 200,
  "blessing": "新婚快乐",
  "clientRequestId": "gift-20260622-001"
}
```

`entrySource` may be:

- `ONLINE_GIFT`
- `ONSITE_QR`

Both paths create `payment_order` and use the same success processing path.

`clientRequestId` is optional but recommended for clients. Reusing the same value returns the existing `payment_order`, preventing duplicate payment-order creation on repeat submit or network retry.

Miniapp gift payment page behavior:

- RSVP success can pass `guestName` into the gift payment page.
- The page offers quick amounts: 66, 88, 100, 188, 288, 520, 666 and 888.
- Blessing templates can be tapped into the blessing field.
- Online gift and onsite QR use different page titles and helper text, but both submit to the same `/api/gifts/payment-orders` endpoint.
- The success page receives `orderNo` and `banquetId`; after mock success it can navigate to the banquet gift list.

## Mock Payment Success

`POST /api/gifts/payment-orders/{orderNo}/mock-success`

Available only when `PAYMENT_MOCK_SUCCESS_ENABLED=true`.

MVP behavior:

1. marks `payment_order` as `PAID`
2. writes `payment_callback_log`
3. writes `gift_record`
4. writes or creates `favor_contact`
5. writes `favor_entry`
6. writes simulated `broadcast_log`

Real provider callback handling remains for payment adapter integration.

The blessing submitted when creating the payment order is persisted on `payment_order.blessing` and copied into `gift_record.blessing` after payment success.

## Gift List And Summary

- `GET /api/gifts?banquetId=1`
- `GET /api/gifts?banquetId=1&source=ONLINE_GIFT`
- `GET /api/gifts?banquetId=1&keyword=张`
- `GET /api/gifts/summary?banquetId=1`

Summary returns total count, total amount, source counts and source amounts.

Miniapp gift list page behavior:

- Amounts are displayed as `¥xx.xx`.
- Gift sources are displayed as Chinese labels: 线上随礼, 现场扫码 and 现金记礼.
- Blessing text is shown with each gift record when present.
- The page supports filtering by guest name and gift source.
- Empty state text changes by current keyword and source filter.
- Reset clears keyword and source filter, then reloads the list.

Admin business gift page behavior:

- Gift list filters support banquet ID, gift source and guest keyword.
- The page shows gift count, total gift amount and source amount summaries from the currently loaded list.
- Reset clears source and keyword, restores the route banquet ID when present, then reloads.
- Offline cash gift validates banquet ID, guest name and amount before submitting to `/api/admin/gifts/offline`.

## Offline Gift

`POST /api/gifts/offline`

Cash gift recording does not create `payment_order`, but still writes:

- `gift_record`
- `favor_contact`
- `favor_entry`
- operation log
- simulated `broadcast_log`

## Favor Ledger

- `GET /api/favor/contacts`
- `GET /api/favor/contacts?keyword=张`
- `GET /api/favor/contacts/{id}`
- `GET /api/favor/compare?contactName=张三`
- `POST /api/favor/manual`

No `favor_compare_snapshot` table is used. Bilateral comparison is computed from `favor_entry`.

Miniapp favor ledger page behavior:

- The contact list shows total received amount, total given amount and current net balance.
- Amounts are displayed as `¥xx.xx`.
- Positive balance means the contact has sent in more; negative balance means the current user has sent out more.
- Manual entry validates contact name and amount before submitting to `/api/favor/manual`.
- Manual entry supports `RECEIVED` and `GIVEN`; optional note is persisted as `favor_entry.note`.
- Bilateral comparison uses `/api/favor/compare` and displays received, given and balance in the same balance semantics.
- Contact detail displays direction, source label, banquet ID, occurrence time and note.
- Favor source labels include 线上随礼, 现场扫码, 现金记礼 and 手动补录.

Admin business RSVP and favor page behavior:

- RSVP filters support banquet ID, status and keyword; reset restores the route banquet ID when present.
- RSVP summary uses `/api/admin/rsvp/stats` after explicit stats query; otherwise it is computed from the currently loaded list.
- Favor contact filters support contact keyword and dedicated export banquet ID.
- Favor summary shows contact count, total received amount, total given amount and net balance from the currently loaded list.
- Favor manual entry validates contact name, amount and optional banquet ID before submitting to `/api/admin/favor/manual`.
- Favor balance semantics are consistent with the miniapp: positive means the contact has sent in more, negative means the current user has sent out more.
