CREATE TABLE IF NOT EXISTS manifest_versions (
    id TINYINT UNSIGNED PRIMARY KEY,
    current_version BIGINT UNSIGNED NOT NULL,
    updated_at DATETIME NOT NULL
);

INSERT INTO manifest_versions (id, current_version, updated_at)
SELECT 1, 1, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM manifest_versions WHERE id = 1
);
