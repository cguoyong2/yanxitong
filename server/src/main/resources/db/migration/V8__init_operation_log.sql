CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    operator_id BIGINT NULL,
    operator_type VARCHAR(32) NOT NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id BIGINT NULL,
    summary VARCHAR(255) NOT NULL,
    detail JSON NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operation_log_tenant_module (tenant_id, module),
    INDEX idx_operation_log_target (target_type, target_id),
    INDEX idx_operation_log_created_at (created_at)
);

