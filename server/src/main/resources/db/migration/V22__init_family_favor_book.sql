CREATE TABLE favor_family_book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    creator_user_id BIGINT NULL,
    book_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_family_book_tenant_status (tenant_id, status)
);

CREATE TABLE favor_family_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    family_book_id BIGINT NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NULL,
    relationship VARCHAR(64) NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    permissions VARCHAR(255) NULL,
    invite_status VARCHAR(32) NOT NULL DEFAULT 'JOINED',
    joined_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_family_member_book (tenant_id, family_book_id),
    INDEX idx_family_member_phone (tenant_id, phone)
);

ALTER TABLE favor_entry
    ADD COLUMN book_scope VARCHAR(32) NOT NULL DEFAULT 'PERSONAL' AFTER source_type,
    ADD COLUMN book_id BIGINT NULL AFTER book_scope,
    ADD COLUMN family_member_id BIGINT NULL AFTER book_id,
    ADD COLUMN operator_member_id BIGINT NULL AFTER family_member_id,
    ADD INDEX idx_favor_entry_book (tenant_id, book_scope, book_id);

ALTER TABLE banquet
    ADD COLUMN favor_book_scope VARCHAR(32) NOT NULL DEFAULT 'PERSONAL' AFTER custom_copywriting,
    ADD COLUMN favor_family_book_id BIGINT NULL AFTER favor_book_scope,
    ADD INDEX idx_banquet_favor_book (tenant_id, favor_book_scope, favor_family_book_id);
