CREATE TABLE config_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT NULL,
    value_type VARCHAR(32) NOT NULL DEFAULT 'STRING',
    description VARCHAR(255) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_config_tenant_key (tenant_id, config_key)
);

CREATE TABLE plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    plan_code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(12, 2) NOT NULL DEFAULT 0,
    price_unit VARCHAR(32) NOT NULL,
    recommended TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_plan_tenant_code (tenant_id, plan_code)
);

CREATE TABLE plan_right (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    plan_id BIGINT NOT NULL,
    right_code VARCHAR(64) NOT NULL,
    right_name VARCHAR(100) NOT NULL,
    right_value VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_plan_right (plan_id, right_code),
    INDEX idx_plan_right_tenant (tenant_id)
);

