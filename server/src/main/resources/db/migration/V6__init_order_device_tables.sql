CREATE TABLE plan_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    banquet_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    price_unit VARCHAR(32) NOT NULL,
    pay_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_plan_order_no (order_no),
    INDEX idx_plan_order_banquet (tenant_id, banquet_id)
);

CREATE TABLE device_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    device_type VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(12, 2) NOT NULL DEFAULT 0,
    price_unit VARCHAR(32) NOT NULL,
    delivery_method VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_device_config_type (tenant_id, device_type, delivery_method)
);

CREATE TABLE device_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    banquet_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    need_device TINYINT NOT NULL DEFAULT 0,
    device_type VARCHAR(32) NOT NULL,
    rent_start_at DATETIME NULL,
    rent_end_at DATETIME NULL,
    price DECIMAL(12, 2) NOT NULL DEFAULT 0,
    price_unit VARCHAR(32) NOT NULL,
    delivery_method VARCHAR(64) NOT NULL,
    pay_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
    order_status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_device_order_no (order_no),
    INDEX idx_device_order_banquet (tenant_id, banquet_id)
);

CREATE TABLE device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    device_type VARCHAR(32) NOT NULL,
    device_code VARCHAR(64) NOT NULL,
    hardware_sn VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_device_code (device_code),
    INDEX idx_device_tenant_type (tenant_id, device_type)
);

CREATE TABLE device_bind (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    banquet_id BIGINT NOT NULL,
    device_id BIGINT NULL,
    device_type VARCHAR(32) NOT NULL,
    bind_code VARCHAR(64) NOT NULL,
    bind_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    bound_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_device_bind_code (bind_code),
    INDEX idx_device_bind_banquet (tenant_id, banquet_id)
);

