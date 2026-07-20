UPDATE config_item
SET config_value = 'https://work.weixin.qq.com/ca/cawcdeb38645437bb2',
    value_type = 'URL',
    description = '小程序专属客服企业微信获客助手链接',
    enabled = 1
WHERE tenant_id IS NULL
  AND config_key = 'customer.service.acquire_link'
  AND deleted = 0;

INSERT INTO config_item (tenant_id, config_key, config_value, value_type, description, enabled)
SELECT NULL,
       'customer.service.acquire_link',
       'https://work.weixin.qq.com/ca/cawcdeb38645437bb2',
       'URL',
       '小程序专属客服企业微信获客助手链接',
       1
WHERE NOT EXISTS (
    SELECT 1
    FROM config_item
    WHERE tenant_id IS NULL
      AND config_key = 'customer.service.acquire_link'
      AND deleted = 0
);
