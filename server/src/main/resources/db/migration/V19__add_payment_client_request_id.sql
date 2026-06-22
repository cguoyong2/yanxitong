ALTER TABLE payment_order
    ADD COLUMN client_request_id VARCHAR(128) NULL AFTER order_no,
    ADD UNIQUE KEY uk_payment_client_request (tenant_id, client_request_id);
