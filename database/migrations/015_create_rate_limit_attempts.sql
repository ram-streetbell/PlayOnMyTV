CREATE TABLE IF NOT EXISTS rate_limit_attempts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    action_key VARCHAR(100) NOT NULL,
    identifier VARCHAR(190) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    window_started_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uq_rate_limit_action_identifier (action_key, identifier)
);
