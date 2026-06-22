UPDATE config_item
SET description = 'P1 已启用 CSV/XLSX 导出能力，业务数据页提供下载入口'
WHERE tenant_id IS NULL
  AND config_key = 'export.excel.mvp.enabled'
  AND deleted = 0;
