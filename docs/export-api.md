# Export API

P1 enables banquet business data export through admin-only CSV and native `.xlsx` endpoints.

## Design Decisions

- Export is admin-only and protected by admin authentication.
- Export is banquet-scoped.
- Export requires the banquet's active plan to include `EXCEL_EXPORT`.
- Current implementation is synchronous CSV and XLSX download.
- CSV is encoded as UTF-8 with BOM so spreadsheet software can open Chinese text directly.
- XLSX uses native workbook files with Chinese sheet names, header styles, column widths and money/date formats.
- Current row cap is 10,000 rows per export file. The service fetches one extra row to detect oversize exports and fails before file generation instead of silently truncating data.
- Each successful export writes an `operation_log` record with module `EXPORT` and detail containing format, type and row count.
- CSV and XLSX reuse the same entitlement check and export table query/mapping boundary.

## Endpoints

### Gift Records

`GET /api/admin/exports/banquets/{banquetId}/gifts.csv`

`GET /api/admin/exports/banquets/{banquetId}/gifts.xlsx`

Columns:

- 礼金ID
- 宴席ID
- 来源
- 来宾姓名
- 金额
- 祝福/备注
- 收礼时间

### RSVP Records

`GET /api/admin/exports/banquets/{banquetId}/rsvp.csv`

`GET /api/admin/exports/banquets/{banquetId}/rsvp.xlsx`

Columns:

- 回执ID
- 宴席ID
- 姓名
- 手机
- 出席状态
- 用餐
- 住宿
- 人数
- 留言
- 提交时间

### Favor Entries

`GET /api/admin/exports/banquets/{banquetId}/favor.csv`

`GET /api/admin/exports/banquets/{banquetId}/favor.xlsx`

Columns:

- 人情ID
- 宴席ID
- 联系人
- 方向
- 来源
- 金额
- 发生时间
- 备注

## Permission Behavior

If the banquet's current active plan does not include `EXCEL_EXPORT`, the API returns:

```json
{
  "code": 403,
  "message": "当前宴席版本不支持导出",
  "data": null
}
```

Seed migration `V14__activate_export_rights.sql` activates `EXCEL_EXPORT` for the plans that already had the reserved P1 right.

## Row Cap Behavior

If an export result would exceed 10,000 rows, the API returns `413` and no success operation log is written:

```json
{
  "code": 413,
  "message": "礼金记录超过导出上限10000行，请缩小范围后重试",
  "data": null
}
```

The message prefix varies by export type:

- `礼金记录`
- `回执记录`
- `人情账本`

## Admin UI

The admin business page provides CSV and XLSX export buttons for gift records, RSVP records and favor entries. Gift and RSVP exports use the current banquet ID filter; favor exports use the dedicated export banquet ID input.

Each export button shows a per-file loading state. Permission and row-cap failures are displayed as admin-facing error messages.
