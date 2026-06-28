SET @media_column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media'
      AND COLUMN_NAME = 'original_filename'
);
SET @media_column_sql := IF(
    @media_column_exists = 0,
    'ALTER TABLE media ADD COLUMN original_filename VARCHAR(255) NOT NULL AFTER title',
    'SELECT 1'
);
PREPARE media_column_stmt FROM @media_column_sql;
EXECUTE media_column_stmt;
DEALLOCATE PREPARE media_column_stmt;

SET @media_idx_business_deleted_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media'
      AND INDEX_NAME = 'idx_media_business_deleted'
);
SET @media_idx_business_deleted_sql := IF(
    @media_idx_business_deleted_exists = 0,
    'ALTER TABLE media ADD INDEX idx_media_business_deleted (business_id, deleted_at)',
    'SELECT 1'
);
PREPARE media_idx_business_deleted_stmt FROM @media_idx_business_deleted_sql;
EXECUTE media_idx_business_deleted_stmt;
DEALLOCATE PREPARE media_idx_business_deleted_stmt;

SET @media_idx_business_type_status_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media'
      AND INDEX_NAME = 'idx_media_business_type_status'
);
SET @media_idx_business_type_status_sql := IF(
    @media_idx_business_type_status_exists = 0,
    'ALTER TABLE media ADD INDEX idx_media_business_type_status (business_id, media_type, status)',
    'SELECT 1'
);
PREPARE media_idx_business_type_status_stmt FROM @media_idx_business_type_status_sql;
EXECUTE media_idx_business_type_status_stmt;
DEALLOCATE PREPARE media_idx_business_type_status_stmt;

SET @media_idx_original_filename_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media'
      AND INDEX_NAME = 'idx_media_original_filename'
);
SET @media_idx_original_filename_sql := IF(
    @media_idx_original_filename_exists = 0,
    'ALTER TABLE media ADD INDEX idx_media_original_filename (original_filename)',
    'SELECT 1'
);
PREPARE media_idx_original_filename_stmt FROM @media_idx_original_filename_sql;
EXECUTE media_idx_original_filename_stmt;
DEALLOCATE PREPARE media_idx_original_filename_stmt;
