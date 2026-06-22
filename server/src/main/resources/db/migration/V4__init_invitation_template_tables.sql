CREATE TABLE template_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    type_code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_template_type_tenant_code (tenant_id, type_code)
);

CREATE TABLE invitation_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    template_code VARCHAR(64) NOT NULL,
    type_code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    cover_url VARCHAR(512) NULL,
    price_type VARCHAR(32) NOT NULL DEFAULT 'FREE',
    price DECIMAL(12, 2) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_invitation_template_tenant_code (tenant_id, template_code),
    INDEX idx_invitation_template_type (tenant_id, type_code)
);

CREATE TABLE invitation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    banquet_id BIGINT NOT NULL,
    template_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    cover_url VARCHAR(512) NULL,
    basic_fields JSON NULL,
    share_slug VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_invitation_share_slug (share_slug),
    INDEX idx_invitation_banquet (tenant_id, banquet_id)
);

CREATE TABLE invitation_share (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    invitation_id BIGINT NOT NULL,
    share_channel VARCHAR(32) NOT NULL,
    share_url VARCHAR(512) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invitation_share_invitation (tenant_id, invitation_id)
);

CREATE TABLE invitation_visit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    invitation_id BIGINT NOT NULL,
    visitor_open_id VARCHAR(128) NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    visited_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invitation_visit_invitation (tenant_id, invitation_id)
);

