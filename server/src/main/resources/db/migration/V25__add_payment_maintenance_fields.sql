ALTER TABLE payment_order
    DROP INDEX uk_payment_client_request,
    ADD COLUMN idempotency_active TINYINT NULL DEFAULT 1 AFTER client_request_id,
    ADD COLUMN last_queried_at DATETIME NULL AFTER expires_at,
    ADD COLUMN query_attempt_count INT NOT NULL DEFAULT 0 AFTER last_queried_at,
    ADD COLUMN next_query_at DATETIME NULL AFTER query_attempt_count,
    ADD COLUMN last_query_error VARCHAR(1000) NULL AFTER next_query_at,
    ADD COLUMN closed_at DATETIME NULL AFTER last_query_error,
    ADD COLUMN close_reason VARCHAR(255) NULL AFTER closed_at,
    ADD UNIQUE KEY uk_payment_active_client_request (tenant_id, client_request_id, idempotency_active),
    ADD INDEX idx_payment_maintenance (pay_status, next_query_at, expires_at);

UPDATE payment_order
SET idempotency_active = CASE
    WHEN pay_status IN ('CREATED', 'PAID') THEN 1
    ELSE NULL
END;
