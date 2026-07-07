ALTER TABLE payment_order
    ADD COLUMN biz_order_type VARCHAR(32) NULL AFTER entry_source,
    ADD COLUMN biz_order_no VARCHAR(64) NULL AFTER biz_order_type,
    ADD INDEX idx_payment_biz_order (tenant_id, biz_order_type, biz_order_no);
