CREATE TABLE IF NOT EXISTS playlists (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    business_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT NULL,
    status ENUM('draft', 'active', 'archived') NOT NULL DEFAULT 'draft',
    is_looping TINYINT(1) NOT NULL DEFAULT 1,
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_playlists_business FOREIGN KEY (business_id) REFERENCES businesses(id),
    CONSTRAINT fk_playlists_user FOREIGN KEY (created_by) REFERENCES users(id)
);

