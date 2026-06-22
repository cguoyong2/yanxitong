UPDATE config_item
SET config_value = '10000',
    value_type = 'NUMBER',
    description = '单次导出最大行数，超过后拒绝生成文件，避免静默截断数据',
    enabled = 1
WHERE tenant_id IS NULL
  AND config_key = 'export.max_rows'
  AND deleted = 0;

INSERT INTO config_item (tenant_id, config_key, config_value, value_type, description, enabled)
SELECT NULL,
       'export.max_rows',
       '10000',
       'NUMBER',
       '单次导出最大行数，超过后拒绝生成文件，避免静默截断数据',
       1
WHERE NOT EXISTS (
    SELECT 1
    FROM config_item
    WHERE tenant_id IS NULL
      AND config_key = 'export.max_rows'
      AND deleted = 0
);
