INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'ELEGANT_WEDDING', 'PAID', '红金雅致婚宴请柬', '', 'PAID', 99.00, 11, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'ELEGANT_WEDDING'
);

INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'WARM_BIRTHDAY', 'FREE', '暖金寿宴请柬', '', 'FREE', 0.00, 31, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'WARM_BIRTHDAY'
);

INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'BABY_GARDEN', 'FREE', '满月花园请柬', '', 'FREE', 0.00, 35, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'BABY_GARDEN'
);

INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'HOUSEWARMING_MODERN', 'PAID', '现代乔迁请柬', '', 'PAID', 69.00, 45, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'HOUSEWARMING_MODERN'
);

INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'SCHOOL_HONOR', 'FREE', '蓝金升学请柬', '', 'FREE', 0.00, 51, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'SCHOOL_HONOR'
);

INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
SELECT NULL, 'MEMORIAL_SIMPLE', 'FREE', '素雅追思请柬', '', 'FREE', 0.00, 61, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM invitation_template WHERE template_code = 'MEMORIAL_SIMPLE'
);
