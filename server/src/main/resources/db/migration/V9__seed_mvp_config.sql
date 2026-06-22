INSERT INTO tenant (id, name, code, status)
VALUES (1, '默认平台', 'default', 'ACTIVE');

INSERT INTO config_item (tenant_id, config_key, config_value, value_type, description, enabled)
VALUES
(NULL, 'system.default.copywriting.gift_success', '心意已收到，感谢您的祝福', 'STRING', '系统默认收礼成功文案', 1),
(NULL, 'export.excel.mvp.enabled', 'false', 'BOOLEAN', 'MVP 不开发正式 Excel 导出，仅预留权益提示', 1);

INSERT INTO theme (tenant_id, theme_code, name, primary_color, secondary_color, icon_style, confirm_screen_template, enabled)
VALUES
(NULL, 'wedding_red_gold', '红金喜庆', '#B91C1C', '#F5C542', 'celebration', 'default_success', 1),
(NULL, 'birthday_warm_gold', '深红暖金', '#8F1D1D', '#D6A84F', 'birthday', 'default_success', 1),
(NULL, 'baby_warm_orange', '暖橙温馨', '#F97316', '#FED7AA', 'family', 'default_success', 1),
(NULL, 'house_orange_gold', '橙金家宅', '#EA580C', '#FACC15', 'home', 'default_success', 1),
(NULL, 'school_blue_gold', '蓝金书卷', '#1D4ED8', '#FBBF24', 'school', 'default_success', 1),
(NULL, 'memorial_gray_black', '灰黑素雅', '#374151', '#111827', 'memorial', 'default_success', 1),
(NULL, 'neutral_warm', '中性暖色', '#A16207', '#FDE68A', 'general', 'default_success', 1);

INSERT INTO event_type (tenant_id, event_type_code, name, alias, default_theme_code, default_copywriting, sort_order, enabled)
VALUES
(NULL, 'WEDDING', '婚宴', '结婚宴', 'wedding_red_gold', '在线随礼，送上祝福', 10, 1),
(NULL, 'BIRTHDAY', '寿宴', '祝寿宴', 'birthday_warm_gold', '祝寿感恩，福寿绵长', 20, 1),
(NULL, 'BABY', '满月', '满月宴', 'baby_warm_orange', '成长、家庭与温暖', 30, 1),
(NULL, 'HOUSEWARMING', '乔迁', '乔迁宴', 'house_orange_gold', '新居乔迁，好运常伴', 40, 1),
(NULL, 'SCHOOL', '升学', '升学宴', 'school_blue_gold', '感谢陪伴，祝贺成长', 50, 1),
(NULL, 'MEMORIAL', '追思会', '追思', 'memorial_gray_black', '敬献心意，表达哀思', 60, 1),
(NULL, 'OTHER', '其他', '通用宴席', 'neutral_warm', '感谢您的到来', 70, 1);

INSERT INTO theme_copywriting (tenant_id, theme_code, event_type_code, scene_code, title, content, speaker_text, enabled)
VALUES
(NULL, 'wedding_red_gold', 'WEDDING', 'GIFT_SUCCESS', '支付成功', '感谢您的祝福，心意已收到', '收到一份新婚祝福', 1),
(NULL, 'birthday_warm_gold', 'BIRTHDAY', 'GIFT_SUCCESS', '心意已收到', '感谢您的祝寿心意', '收到一份祝寿心意', 1),
(NULL, 'baby_warm_orange', 'BABY', 'GIFT_SUCCESS', '心意已收到', '感谢您为宝宝送上的温暖祝福', '收到一份满月祝福', 1),
(NULL, 'house_orange_gold', 'HOUSEWARMING', 'GIFT_SUCCESS', '心意已收到', '感谢您送上的乔迁祝福', '收到一份乔迁祝福', 1),
(NULL, 'school_blue_gold', 'SCHOOL', 'GIFT_SUCCESS', '心意已收到', '感谢您送上的升学祝贺', '收到一份升学祝贺', 1),
(NULL, 'memorial_gray_black', 'MEMORIAL', 'GIFT_SUCCESS', '心意已收到', '感谢您表达的哀思', '收到一份追思心意', 1),
(NULL, 'neutral_warm', 'OTHER', 'GIFT_SUCCESS', '心意已收到', '感谢您的心意', '收到一份宴席心意', 1);

INSERT INTO plan (tenant_id, plan_code, name, price, price_unit, recommended, sort_order, status)
VALUES
(NULL, 'BASIC', '基础版', 0.00, '场', 0, 10, 'ACTIVE'),
(NULL, 'PRO', '专业版', 299.00, '场', 1, 20, 'ACTIVE'),
(NULL, 'PREMIUM', '至尊尊享版', 699.00, '场', 0, 30, 'ACTIVE');

INSERT INTO plan_right (tenant_id, plan_id, right_code, right_name, right_value)
SELECT NULL, id, 'EXCEL_EXPORT', 'Excel 导出', 'P1_RESERVED' FROM plan WHERE plan_code IN ('PRO', 'PREMIUM');

INSERT INTO template_type (tenant_id, type_code, name, sort_order, enabled)
VALUES
(NULL, 'FREE', '免费模板', 10, 1),
(NULL, 'PAID', '收费模板', 20, 1),
(NULL, 'CUSTOM', '定制模板', 30, 1);

INSERT INTO invitation_template (tenant_id, template_code, type_code, name, cover_url, price_type, price, sort_order, status)
VALUES
(NULL, 'DEFAULT_WEDDING', 'FREE', '默认婚宴请柬', '', 'FREE', 0.00, 10, 'ACTIVE'),
(NULL, 'DEFAULT_GENERAL', 'FREE', '默认通用请柬', '', 'FREE', 0.00, 20, 'ACTIVE');

INSERT INTO device_config (tenant_id, device_type, name, price, price_unit, delivery_method, enabled)
VALUES
(NULL, 'CLOUD_SPEAKER', '云喇叭', 99.00, '场', '同城配送', 1),
(NULL, 'CONFIRM_SCREEN', '收礼确认屏', 199.00, '场', '同城配送', 1);

