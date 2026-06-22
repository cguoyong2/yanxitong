# 确认屏与云喇叭模拟

## 范围

MVP 阶段确认屏不强依赖真实硬件 SN。确认屏通过 `bindCode` 与 `banquetId` 绑定，待机页通过 WebSocket 监听该宴席的礼金支付成功事件。

云喇叭第一阶段不接入真实设备，只在礼金成功后写入 `broadcast_log`，用于模拟播报日志和后台排查。

## 接口

### 绑定确认屏

`POST /api/confirm-screen/bind`

```json
{
  "banquetId": 1,
  "bindCode": "CS-20260622"
}
```

返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "banquetId": 1,
    "bindCode": "CS-20260622",
    "bindStatus": "BOUND",
    "deviceType": "CONFIRM_SCREEN"
  }
}
```

### 查询绑定状态

`GET /api/confirm-screen/status/{bindCode}`

返回中包含：

- `online`：当前是否有确认屏 WebSocket 在线。
- `onlineSessions`：该宴席当前在线确认屏连接数。

### 查询最近礼金事件

`GET /api/confirm-screen/banquets/{banquetId}/latest-event`

用于确认屏刷新、重新连接或短暂离线后读取最近一条礼金成功事件。

### 礼金事件推送

WebSocket 地址：

`/ws/confirm-screen?banquetId={banquetId}`

礼金成功后推送：

```json
{
  "type": "GIFT_PAID",
  "banquetId": 1,
  "giftRecordId": 10,
  "guestName": "张三",
  "amount": 666.00,
  "message": "新婚快乐",
  "paidAt": "2026-06-22T12:00:00"
}
```

## 前端页面

- `/bind`：输入宴席 ID 和绑定码，调用绑定接口。
- `/standby`：读取本地绑定信息，建立 WebSocket，等待礼金成功事件。
- `/standby`：展示现场待机视觉、在线状态、在线连接数和最近到账事件。
- `/success`：展示最近一条礼金成功事件，包含大字号金额、祝福语和返回倒计时，8 秒后返回待机页。
- `/offline`：WebSocket 断开后进入，可重新连接或重新绑定，并提示可通过最近事件接口恢复最新到账记录。

## 事件来源

确认屏展示事件不单独建表，由 `gift_record` 支付成功派生。成功事件同时触发：

- `broadcast_log` 写入云喇叭模拟日志。
- WebSocket 推送确认屏当前支付成功页。
- `broadcast_log` 写入确认屏推送日志，状态为 `PUSHED` 或 `OFFLINE`。

## 后台排查

`GET /api/admin/broadcast-logs`

可选查询参数：

- `banquetId`
- `giftRecordId`
- `deviceType`：`CLOUD_SPEAKER` 或 `CONFIRM_SCREEN`
- `eventType`：`GIFT_PAID`
- `status`：`SIMULATED`、`PUSHED`、`OFFLINE`

后台播报日志页支持按宴席、礼金、设备类型、事件类型和状态筛选，并显示云喇叭模拟数、确认屏已推送数和确认屏离线数。操作日志中的礼金相关动作可跳转到播报日志并带入礼金 ID，用于排查礼金成功后的确认屏和云喇叭链路。
