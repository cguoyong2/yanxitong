CREATE TABLE banquet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    owner_user_id BIGINT NULL,
    banquet_no VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    event_type_code VARCHAR(64) NOT NULL,
    theme_code VARCHAR(64) NOT NULL,
    banquet_time DATETIME NULL,
    location VARCHAR(255) NULL,
    custom_copywriting JSON NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_banquet_no (banquet_no),
    INDEX idx_banquet_tenant_owner (tenant_id, owner_user_id),
    INDEX idx_banquet_theme (tenant_id, theme_code)
);

CREATE TABLE rsvp_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    banquet_id BIGINT NOT NULL,
    invitation_id BIGINT NULL,
    guest_name VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NULL,
    attendance_status VARCHAR(32) NOT NULL,
    meal_required TINYINT NOT NULL DEFAULT 0,
    accommodation_required TINYINT NOT NULL DEFAULT 0,
    guest_count INT NOT NULL DEFAULT 1,
    message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_rsvp_banquet (tenant_id, banquet_id)
);

