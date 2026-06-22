UPDATE config_item
SET config_value = 'true',
    description = 'P1 启用 CSV/Excel 导出能力，当前后端先提供 CSV 下载边界'
WHERE config_key = 'export.excel.mvp.enabled';

UPDATE plan_right
SET right_value = 'INCLUDED',
    right_name = 'CSV/Excel 导出'
WHERE right_code = 'EXCEL_EXPORT'
  AND right_value = 'P1_RESERVED';
