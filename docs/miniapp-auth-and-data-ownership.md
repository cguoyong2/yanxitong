# 小程序认证与数据归属

## 登录会话

小程序通过 `POST /api/wechat/miniapp/login` 提交 `uni.login` 返回的临时 `code`。后端完成 `code2session` 后创建或更新 `miniapp_user`，并返回 Redis 会话令牌。小程序后续请求统一携带：

```text
Authorization: Bearer <miniapp-token>
```

会话有效期为 7 天，有效访问会刷新 Redis 过期时间。客户端收到 `401` 后会清理旧会话并自动重新登录一次。

## 接口边界

以下接口保持游客可访问：

- 已发布请柬公开页。
- 已发布宴席的 RSVP 提交。
- 已发布宴席的线上随礼和现场扫码支付订单创建。
- 公开宴席类型、请柬模板、版本和设备配置查询。
- 微信支付回调。

宴席管理、请柬编辑、RSVP 统计、线下记礼、人情账本、版本订单和设备订单必须登录，并校验当前用户是宴席所有者或有效成员。无权访问统一按不存在处理，避免泄露其他用户数据。

## 草稿与发布

新建宴席和基础请柬均为草稿。宴席执行 `POST /api/banquets/{id}/publish` 后，宴席变为 `PUBLISHED`，请柬同步变为 `ACTIVE`。匿名用户不能查看草稿；宴席所有者携带登录令牌时可以预览草稿。

## 升级前历史数据

系统不会把旧数据自动分配给首位登录用户。部署迁移后按以下步骤操作：

1. 使用实际主账号进入一次小程序，完成用户注册。
2. 登录运营后台，进入“小程序用户”。
3. 核对最近登录时间和用户 ID。
4. 点击“接管历史数据”并确认。

该操作只处理所有者为空的宴席、人情联系人和家庭账本，不覆盖已有归属；重复执行不会重复迁移。操作结果写入 `operation_log`，动作码为 `CLAIM_LEGACY_MINIAPP_DATA`。

## 数据表

- `miniapp_user`：小程序微信身份和状态。
- `banquet.owner_user_id`：宴席所有者。
- `banquet_member`：宴席成员、角色和权限预留。
- `favor_contact.owner_user_id`：个人人情联系人所有者。
- `favor_family_book.creator_user_id`：家庭账本创建者。
- `favor_family_member.user_id`：家庭成员对应的小程序用户。
