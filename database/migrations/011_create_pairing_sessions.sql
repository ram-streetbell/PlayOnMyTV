CREATE TABLE IF NOT EXISTS pairing_sessions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT UNSIGNED NOT NULL,
    pairing_code VARCHAR(12) NOT NULL,
    status ENUM('pending', 'paired', 'expired', 'cancelled') NOT NULL DEFAULT 'pending',
    expires_at DATETIME NOT NULL,
    paired_by_user_id BIGINT UNSIGNED NULL,
    paired_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_pairing_sessions_code_status (pairing_code, status),
    INDEX idx_pairing_sessions_device_status (device_id, status),
    CONSTRAINT fk_pairing_sessions_device FOREIGN KEY (device_id) REFERENCES devices(id),
    CONSTRAINT fk_pairing_sessions_user FOREIGN KEY (paired_by_user_id) REFERENCES users(id)
);

