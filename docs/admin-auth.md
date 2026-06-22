# 后台登录鉴权

## MVP 范围

后台第一阶段仅面向平台运营管理员。代理、酒店、婚庆独立工作台暂不开发，相关租户和角色字段先保留。

MVP 鉴权采用轻量实现：

- `admin_user` 存储平台管理员。
- `/api/auth/login` 校验账号密码并签发 Bearer Token。
- Token 存入 Redis，默认 12 小时有效，请求时滑动续期。
- `/api/admin/**` 接口必须携带 `Authorization: Bearer {token}`。
- 前台公开接口、小程序接口、确认屏绑定和 WebSocket 不走后台管理员鉴权。

## 默认账号

本地初始化迁移会创建默认平台管理员：

- 账号：`admin`
- 密码：`admin123`
- 租户：`1`

该账号仅用于 MVP 本地初始化。生产部署前应更换密码或改为部署脚本创建。

## 接口

### 登录

`POST /api/auth/login`

```json
{
  "username": "admin",
  "password": "admin123"
}
```

返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "token",
    "adminUserId": 1,
    "tenantId": 1,
    "username": "admin",
    "displayName": "平台管理员"
  }
}
```

### 管理端请求头

```http
Authorization: Bearer {token}
```

## 操作日志

登录会记录 `operation_log`：

- `module`: `AUTH`
- `action`: `LOGIN`
- `operator_type`: `ADMIN`

管理端登录后的关键操作会从当前管理员上下文中写入 `operator_id/operator_type`。

后台操作日志接口：

`GET /api/admin/operation-logs`

可选查询参数：

- `module`
- `action`
- `targetType`
- `targetId`
- `keyword`

后台操作日志页支持按模块、动作、对象类型、对象 ID 和摘要/详情关键词筛选，并显示当前结果的日志条数、涉及模块数、系统动作数和管理员动作数。礼金和支付相关操作可跳转到播报日志页辅助排查确认屏/云喇叭链路。
