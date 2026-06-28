# Miniapp Experience Regression Checklist

Date: 2026-06-28

This checklist is for every WeChat DevTools preview, experience-version upload, or production miniapp smoke pass. It focuses on the real user journey after the non-payment MVP polish work.

## Preconditions

- Backend API is reachable at the configured miniapp API base URL.
- WeChat DevTools imports `miniapp/dist/build/mp-weixin`.
- AppID is `wx5cbc30150256d707`.
- Request legal domain contains `https://yxt.yqej.cn`.
- Production and experience builds must keep real payment disabled until WeChat service-provider or payment-institution credentials are ready.

## Home

1. Open the miniapp home page.
2. Confirm the red hero area, banner, event type cards and latest banquet card render without horizontal clipping.
3. Tap each tab: `宴席`, `人情`, `请柬`, `我的`.
4. Confirm the latest banquet card uses real backend data, not placeholder values.
5. Tap `创建宴席`; it must open the create page.

## Create Banquet

1. Confirm the form starts empty unless `填入体验数据` is tapped.
2. Switch banquet types: wedding, birthday, baby, housewarming, school, memorial.
3. Confirm theme color, mark and template recommendation change with the type.
4. Select a template loaded from backend metadata.
5. Fill banquet name, time, location, host name and phone.
6. Create the banquet.
7. Expected result: banquet detail page opens.
8. Expected data: corresponding invitation basic fields include host name, phone and address detail.

## Banquet Detail

1. Confirm the red hero area, overview card, stats and action grid render consistently.
2. Tap `发请柬`; public invitation page opens.
3. Tap `编辑字段`; invitation editor opens.
4. Tap `回执统计`, `线下记礼`, `收礼记录`, `人情账本`, `选择版本`, `设备选择`; every entry must open the expected page or a clear non-payment entitlement prompt.
5. Online gift must show a disabled/non-payment state when production mock payment is off.

## Invitation Tab

1. Open the `请柬` tab.
2. Confirm templates are loaded from `/meta/invitation-templates`.
3. Switch invitation types and filters: `全部`, `免费`, `付费`, `定制`, `热门`.
4. Empty filters must show a clear empty state, not a blank area.
5. Tap `使用`; create banquet page opens with the selected type and template preselected.
6. Open `我的请柬`; public invitation page opens with the latest share slug.

## Invitation Editor

1. Edit title, host name, contact phone, cover URL, address detail, greeting and schedule.
2. Toggle gift and device entries.
3. Save.
4. Expected result: remain on the editor page.
5. Tap `预览请柬`; public invitation page opens.
6. Tap `复制路径`; clipboard receives the miniapp share path.

## Public Invitation

1. Confirm cover, title, greeting, banquet time, location, host and phone render correctly.
2. Tap WeChat share; shared path must include the current `slug`.
3. Tap `回执出席`; RSVP page opens with banquet and invitation IDs.
4. In non-payment experience mode, `在线随礼` must show a clear disabled message.

## RSVP

1. Submit an attending RSVP with meal count.
2. Submit pending and declined states.
3. Phone format validation must reject invalid phone numbers.
4. After success, `返回请柬` must return to the public invitation page when invitation ID exists.
5. In non-payment experience mode, the primary success action must be `去线下记礼`, not online payment.
6. Open RSVP stats and confirm counts update.

## Gift Flow

1. Open `线下记礼`.
2. Enter guest name, amount and blessing.
3. Save.
4. Expected result: success modal appears with `继续登记` and `查看记录`.
5. Open gift records and confirm the saved record appears.
6. Search by guest name and filter by source.
7. Summary totals must not crash when a source amount is missing.

## Favor Ledger

1. Open `人情`.
2. Confirm totals and recent contacts render from backend data.
3. Add a manual received record and a manual given record.
4. Open contact detail.
5. Confirm the detail page handles empty or missing contact fields without white screen.
6. Open `家庭人情`; it should clearly state that family collaboration is a later feature.

## Plan And Device

1. Open `选择版本`.
2. Current version button must be disabled and must not create a duplicate order.
3. Create a paid plan order in mock-enabled environments only.
4. Open `设备选择`.
5. Without device rights, rent buttons must block and guide to version selection.
6. With device rights, choose rent date/time.
7. End time earlier than or equal to start time must be rejected.
8. Valid order should appear in `已租设备`.

## Mine

1. Open `我的`.
2. Confirm banquet and invitation counts come from backend data.
3. Tap service entries: banquet, invitation, gift, favor, plan and device.
4. Entries requiring a banquet should use the latest banquet fallback or show `请先创建宴席`.
5. Labels must read `绑定记录` and `交付说明`.

## Non-Payment Production Boundary

In production or experience-version builds before real WeChat payment launch:

- Online gift and onsite QR payment must not complete real payment.
- Mock-success controls must remain hidden.
- Gift payment page must guide users to offline gift recording.
- Payment-success page must not allow mock success without an order number.

## Issue Record Template

For each issue, record:

- Build or preview QR file path.
- Device model and WeChat version.
- Page path.
- Operation steps.
- Expected result.
- Actual result.
- Screenshot or screen recording.
