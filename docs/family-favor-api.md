# 家庭人情簿 API

V1.4 家庭人情簿 MVP 在个人 `favor` 模块内新增家庭账本子域，先支持基础协作闭环：

- 创建家庭人情簿。
- 查询我参与的家庭人情簿列表。
- 添加家庭成员。
- 家庭人情手动补录。
- 家庭往来对象汇总。
- 家庭往来对比。
- 宴席可通过 `favorBookScope=FAMILY` 和 `favorFamilyBookId` 归属家庭账本。

## 数据归属

`favor_entry` 新增字段：

- `book_scope`: `PERSONAL` 或 `FAMILY`。
- `book_id`: 家庭账本 ID，个人账本为空。
- `family_member_id`: 录入家庭成员 ID。
- `operator_member_id`: 经办家庭成员 ID。

宴席新增字段：

- `favor_book_scope`: 默认 `PERSONAL`。
- `favor_family_book_id`: 家庭账本 ID。

礼金成功或线下记礼时，若宴席归属家庭账本，则自动写入 `book_scope=FAMILY` 的人情流水。

## 接口

`GET /api/favor/family-books`

返回家庭账本列表及每个账本的成员和汇总。

`POST /api/favor/family-books`

创建家庭账本，并自动添加 OWNER 成员。

```json
{
  "bookName": "陈家人情簿",
  "description": "家庭共同维护",
  "ownerName": "陈先生",
  "ownerPhone": "13800000000"
}
```

`GET /api/favor/family-books/{id}`

查询家庭账本详情和统计。

`POST /api/favor/family-books/{id}/members`

添加家庭成员。

```json
{
  "memberName": "李女士",
  "phone": "13900000000",
  "relationship": "配偶",
  "role": "MEMBER"
}
```

`GET /api/favor/family-books/{id}/contacts?keyword=张`

查询家庭往来对象汇总。

`GET /api/favor/family-books/{id}/contacts/{contactId}`

查询某个联系人在家庭账本中的流水和差额。

`GET /api/favor/family-books/{id}/compare?contactName=张三`

按姓名查询家庭往来对比。

`POST /api/favor/family-books/{id}/manual`

家庭人情补录。

```json
{
  "contactName": "张三",
  "phone": "13800000000",
  "direction": "RECEIVED",
  "amount": 600,
  "note": "家庭共同补录",
  "familyMemberId": 1,
  "operatorMemberId": 1
}
```

## 操作日志

以下关键操作写入 `operation_log`，模块为 `FAVOR`：

- `CREATE_FAMILY_BOOK`
- `ADD_FAMILY_MEMBER`
- `FAMILY_MANUAL_ENTRY`
