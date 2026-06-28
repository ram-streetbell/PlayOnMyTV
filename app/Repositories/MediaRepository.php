<?php

declare(strict_types=1);

namespace App\Repositories;

class MediaRepository extends BaseRepository
{
    public function listRecentByBusiness(int $businessId, int $limit = 48): array
    {
        $statement = $this->connection->prepare(
            'SELECT id,
                    title,
                    original_filename,
                    media_type,
                    mime_type,
                    file_size_bytes,
                    duration_seconds,
                    width,
                    height,
                    storage_url,
                    thumbnail_url,
                    status,
                    created_at
             FROM media
             WHERE business_id = :business_id
               AND deleted_at IS NULL
             ORDER BY created_at DESC, id DESC
             LIMIT :limit'
        );
        $statement->bindValue(':business_id', $businessId, \PDO::PARAM_INT);
        $statement->bindValue(':limit', $limit, \PDO::PARAM_INT);
        $statement->execute();

        return $statement->fetchAll() ?: [];
    }

    public function create(array $attributes): int
    {
        $statement = $this->connection->prepare(
            'INSERT INTO media (
                business_id,
                title,
                original_filename,
                description,
                media_type,
                mime_type,
                file_size_bytes,
                duration_seconds,
                width,
                height,
                checksum_sha256,
                storage_provider,
                storage_key,
                storage_url,
                thumbnail_url,
                status,
                created_by,
                created_at,
                updated_at,
                deleted_at
            ) VALUES (
                :business_id,
                :title,
                :original_filename,
                :description,
                :media_type,
                :mime_type,
                :file_size_bytes,
                :duration_seconds,
                :width,
                :height,
                :checksum_sha256,
                :storage_provider,
                :storage_key,
                :storage_url,
                :thumbnail_url,
                :status,
                :created_by,
                :created_at,
                :updated_at,
                NULL
            )'
        );
        $statement->execute($attributes);

        return (int) $this->connection->lastInsertId();
    }

    public function findByIdForBusiness(int $mediaId, int $businessId, bool $includeDeleted = false): ?array
    {
        $sql = 'SELECT * FROM media WHERE id = :id AND business_id = :business_id';
        if (!$includeDeleted) {
            $sql .= ' AND deleted_at IS NULL';
        }
        $sql .= ' LIMIT 1';

        $statement = $this->connection->prepare($sql);
        $statement->execute([
            'id' => $mediaId,
            'business_id' => $businessId,
        ]);

        $media = $statement->fetch();

        return is_array($media) ? $media : null;
    }
}
