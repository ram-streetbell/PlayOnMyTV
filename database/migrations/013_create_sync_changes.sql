CREATE TABLE IF NOT EXISTS sync_changes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    business_id BIGINT UNSIGNED NOT NULL,
    entity_type ENUM('media', 'playlist', 'playlist_item', 'schedule', 'schedule_slot', 'device_assignment') NOT NULL,
    entity_id BIGINT UNSIGNED NOT NULL,
    change_type ENUM('created', 'updated', 'deleted') NOT NULL,
    version_no BIGINT UNSIGNED NOT NULL,
    payload_json JSON NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_sync_changes_business FOREIGN KEY (business_id) REFERENCES businesses(id)
);

