<?php

declare(strict_types=1);

namespace App\Repositories;

class SyncChangeRepository extends BaseRepository
{
    public function createChange(
        int $businessId,
        string $entityType,
        int $entityId,
        string $changeType,
        ?array $payload,
        string $createdAt
    ): int {
        $statement = $this->connection->prepare(
            'INSERT INTO sync_changes (
                business_id,
                entity_type,
                entity_id,
                change_type,
                version_no,
                payload_json,
                created_at
            ) VALUES (
                :business_id,
                :entity_type,
                :entity_id,
                :change_type,
                :version_no,
                :payload_json,
                :created_at
            )'
        );

        $statement->execute([
            'business_id' => $businessId,
            'entity_type' => $entityType,
            'entity_id' => $entityId,
            'change_type' => $changeType,
            'version_no' => 0,
            'payload_json' => $payload === null ? null : json_encode($payload, JSON_UNESCAPED_SLASHES),
            'created_at' => $createdAt,
        ]);

        $changeId = (int) $this->connection->lastInsertId();

        $updateStatement = $this->connection->prepare(
            'UPDATE sync_changes SET version_no = :version_no WHERE id = :id'
        );
        $updateStatement->execute([
            'version_no' => $changeId,
            'id' => $changeId,
        ]);

        return $changeId;
    }
}

