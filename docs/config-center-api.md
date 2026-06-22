# Configuration Center API

All endpoints return `ApiResponse<T>`:

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

## CRUD Endpoints

Each endpoint supports:

- `GET <endpoint>`: list records
- `POST <endpoint>`: create when `id` is empty, update when `id` is present
- `DELETE <endpoint>/{id}`: logic delete where table supports `deleted`

## Resources

| Resource | Endpoint | Operation Module |
| --- | --- | --- |
| Generic config items | `/api/admin/config-items` | `CONFIG` |
| Plans | `/api/admin/plans` | `PLAN` |
| Plan rights | `/api/admin/plan-rights` | `PLAN` |
| Event types | `/api/admin/event-types` | `EVENT_TYPE` |
| Themes | `/api/admin/themes` | `THEME` |
| Theme copywriting | `/api/admin/theme-copywriting` | `THEME` |
| Template types | `/api/admin/template-types` | `TEMPLATE` |
| Invitation templates | `/api/admin/invitation-templates` | `TEMPLATE` |
| Device configs | `/api/admin/device-configs` | `DEVICE` |
| Operation logs | `/api/admin/operation-logs` | read-only |

## Confirmed Rules

- Prices, units, version names, rights, templates, themes and devices are maintained here and must not be hard-coded in clients.
- `theme_copywriting` is maintained independently.
- Copywriting priority remains: banquet custom copywriting, theme copywriting, event type default copywriting, system default copywriting.
- Operation logs are written for create, update and delete operations on configuration resources.
- CSV/XLSX export is enabled through `EXCEL_EXPORT`; config and rights remain backend-maintained.
- Export row cap is maintained through generic config item `export.max_rows`; default value is `10000`.
