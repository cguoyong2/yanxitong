CREATE TABLE event_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    event_type_code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    alias VARCHAR(100) NULL,
    default_theme_code VARCHAR(64) NOT NULL,
    default_copywriting TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_event_type_tenant_code (tenant_id, event_type_code)
);

CREATE TABLE theme (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    theme_code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    primary_color VARCHAR(32) NOT NULL,
    secondary_color VARCHAR(32) NULL,
    icon_style VARCHAR(64) NULL,
    confirm_screen_template VARCHAR(64) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_theme_tenant_code (tenant_id, theme_code)
);

CREATE TABLE theme_copywriting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    theme_code VARCHAR(64) NOT NULL,
    event_type_code VARCHAR(64) NOT NULL,
    scene_code VARCHAR(64) NOT NULL,
    title VARCHAR(255) NULL,
    content TEXT NULL,
    speaker_text VARCHAR(255) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_theme_copywriting_scene (tenant_id, theme_code, event_type_code, scene_code)
);

