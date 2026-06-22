CREATE TABLE payment_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    banquet_id BIGINT NULL,
    order_no VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    scene VARCHAR(32) NOT NULL,
    entry_source VARCHAR(32) NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    subject VARCHAR(255) NOT NULL,
    payer_name VARCHAR(100) NULL,
    payer_open_id VARCHAR(128) NULL,
    provider_trade_no VARCHAR(128) NULL,
    pay_status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_order_no (order_no),
    INDEX idx_payment_order_banquet (tenant_id, banquet_id),
    INDEX idx_payment_order_scene (tenant_id, scene, entry_source)
);

CREATE TABLE payment_callback_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    provider VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NULL,
    provider_trade_no VARCHAR(128) NULL,
    raw_body MEDIUMTEXT NOT NULL,
    signature VARCHAR(512) NULL,
    verify_status VARCHAR(32) NOT NULL,
    process_status VARCHAR(32) NOT NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_callback_order (tenant_id, order_no)
);

CREATE TABLE gift_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    banquet_id BIGINT NOT NULL,
    payment_order_id BIGINT NULL,
    gift_source VARCHAR(32) NOT NULL,
    guest_name VARCHAR(100) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    blessing VARCHAR(500) NULL,
    received_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_gift_banquet (tenant_id, banquet_id),
    INDEX idx_gift_payment_order (payment_order_id)
);

CREATE TABLE favor_contact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    owner_user_id BIGINT NULL,
    contact_name VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_favor_contact_name (tenant_id, owner_user_id, contact_name)
);

CREATE TABLE favor_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    contact_id BIGINT NOT NULL,
    banquet_id BIGINT NULL,
    gift_record_id BIGINT NULL,
    direction VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    occurred_at DATETIME NOT NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_favor_entry_contact (tenant_id, contact_id),
    INDEX idx_favor_entry_banquet (tenant_id, banquet_id)
);

CREATE TABLE broadcast_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    banquet_id BIGINT NOT NULL,
    gift_record_id BIGINT NULL,
    device_type VARCHAR(32) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    content VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SIMULATED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_broadcast_banquet (tenant_id, banquet_id)
);

