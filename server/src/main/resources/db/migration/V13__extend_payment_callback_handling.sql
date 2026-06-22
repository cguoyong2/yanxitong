ALTER TABLE payment_callback_log
    ADD COLUMN handle_remark VARCHAR(1000) NULL AFTER error_message,
    ADD COLUMN handled_at DATETIME NULL AFTER handle_remark,
    ADD INDEX idx_payment_callback_status (tenant_id, process_status, verify_status);
