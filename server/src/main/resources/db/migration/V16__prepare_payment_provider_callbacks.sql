ALTER TABLE payment_order
    ADD COLUMN prepay_id VARCHAR(128) NULL AFTER provider_trade_no,
    ADD COLUMN pay_payload MEDIUMTEXT NULL AFTER prepay_id,
    ADD COLUMN provider_status VARCHAR(64) NULL AFTER pay_payload,
    ADD COLUMN expires_at DATETIME NULL AFTER provider_status,
    ADD COLUMN notify_url VARCHAR(500) NULL AFTER expires_at,
    ADD INDEX idx_payment_provider_status (provider, provider_status);

ALTER TABLE payment_callback_log
    ADD COLUMN request_id VARCHAR(128) NULL AFTER provider,
    ADD COLUMN provider_event_id VARCHAR(128) NULL AFTER provider_trade_no,
    ADD COLUMN provider_serial_no VARCHAR(128) NULL AFTER provider_event_id,
    ADD COLUMN event_type VARCHAR(128) NULL AFTER provider_serial_no,
    ADD COLUMN resource_type VARCHAR(128) NULL AFTER event_type,
    ADD COLUMN headers MEDIUMTEXT NULL AFTER resource_type,
    ADD COLUMN decrypted_body MEDIUMTEXT NULL AFTER raw_body,
    ADD INDEX idx_payment_callback_event (provider, provider_event_id);
