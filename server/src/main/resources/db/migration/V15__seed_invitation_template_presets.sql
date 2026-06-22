INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'DEFAULT_BIRTHDAY', 'FREE', '默认寿宴请柬', '', 'FREE', 0.00, 30, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'DEFAULT_BIRTHDAY'
);

INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'DEFAULT_SCHOOL', 'FREE', '默认升学请柬', '', 'FREE', 0.00, 40, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'DEFAULT_SCHOOL'
);

INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'PREMIUM_CEREMONY', 'PAID', '高级典礼请柬', '', 'PAID', 99.00, 50, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'PREMIUM_CEREMONY'
);

INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'CUSTOM_BRAND', 'CUSTOM', '定制品牌请柬', '', 'PLAN_INCLUDED', 0.00, 60, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'CUSTOM_BRAND'
);
