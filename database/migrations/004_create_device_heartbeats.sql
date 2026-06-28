CREATE TABLE IF NOT EXISTS device_heartbeats (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT UNSIGNED NOT NULL,
    online_status ENUM('online', 'offline', 'degraded') NOT NULL DEFAULT 'online',
    current_playlist_id BIGINT UNSIGNED NULL,
    current_media_id BIGINT UNSIGNED NULL,
    free_storage_bytes BIGINT NULL,
    network_type VARCHAR(50) NULL,
    battery_level INT NULL,
    app_version VARCHAR(50) NULL,
    payload_json JSON NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_heartbeats_device FOREIGN KEY (device_id) REFERENCES devices(id)
);

