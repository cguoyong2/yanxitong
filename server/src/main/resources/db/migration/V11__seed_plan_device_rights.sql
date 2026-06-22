INSERT INTO plan_right (tenant_id, plan_id, right_code, right_name, right_value)
SELECT NULL, p.id, 'DEVICE_RENTAL', '设备租赁', 'INCLUDED'
FROM plan p
WHERE p.plan_code IN ('PRO', 'PREMIUM')
  AND NOT EXISTS (
      SELECT 1 FROM plan_right r WHERE r.plan_id = p.id AND r.right_code = 'DEVICE_RENTAL'
  );

INSERT INTO plan_right (tenant_id, plan_id, right_code, right_name, right_value)
SELECT NULL, p.id, 'CONFIRM_SCREEN', '收礼确认屏', 'INCLUDED'
FROM plan p
WHERE p.plan_code IN ('PRO', 'PREMIUM')
  AND NOT EXISTS (
      SELECT 1 FROM plan_right r WHERE r.plan_id = p.id AND r.right_code = 'CONFIRM_SCREEN'
  );

INSERT INTO plan_right (tenant_id, plan_id, right_code, right_name, right_value)
SELECT NULL, p.id, 'CLOUD_SPEAKER', '云喇叭', 'INCLUDED'
FROM plan p
WHERE p.plan_code IN ('PRO', 'PREMIUM')
  AND NOT EXISTS (
      SELECT 1 FROM plan_right r WHERE r.plan_id = p.id AND r.right_code = 'CLOUD_SPEAKER'
  );
