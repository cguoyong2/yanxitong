CREATE TABLE miniapp_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    open_id VARCHAR(128) NOT NULL,
    union_id VARCHAR(128) NULL,
    nickname VARCHAR(100) NULL,
    avatar_url VARCHAR(500) NULL,
    phone VARCHAR(32) NULL,
    role_code VARCHAR(32) NOT NULL DEFAULT 'USER',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_miniapp_user_open_id (open_id),
    INDEX idx_miniapp_user_tenant_status (tenant_id, status)
);

CREATE TABLE banquet_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    banquet_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    permissions VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_banquet_member_user (banquet_id, user_id),
    INDEX idx_banquet_member_user_status (user_id, status),
    INDEX idx_banquet_member_tenant_banquet (tenant_id, banquet_id)
);

ALTER TABLE favor_family_member
    ADD COLUMN user_id BIGINT NULL AFTER family_book_id,
    ADD INDEX idx_family_member_user (tenant_id, user_id, invite_status);
