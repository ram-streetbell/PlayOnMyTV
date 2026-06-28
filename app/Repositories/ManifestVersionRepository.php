<?php

declare(strict_types=1);

namespace App\Repositories;

class ManifestVersionRepository extends BaseRepository
{
    public function ensureInitialized(string $createdAt): void
    {
        $statement = $this->connection->prepare(
            'INSERT INTO manifest_versions (id, current_version, updated_at)
             SELECT 1, 1, :updated_at
             WHERE NOT EXISTS (
                SELECT 1 FROM manifest_versions WHERE id = 1
             )'
        );
        $statement->execute([
            'updated_at' => $createdAt,
        ]);
    }

    public function getCurrent(): array
    {
        $statement = $this->connection->prepare(
            'SELECT id, current_version, updated_at
             FROM manifest_versions
             WHERE id = 1
             LIMIT 1'
        );
        $statement->execute();
        $row = $statement->fetch();

        return is_array($row)
            ? $row
            : [
                'id' => 1,
                'current_version' => 1,
                'updated_at' => date('Y-m-d H:i:s'),
            ];
    }

    public function increment(string $updatedAt): int
    {
        $this->ensureInitialized($updatedAt);

        $statement = $this->connection->prepare(
            'UPDATE manifest_versions
             SET current_version = current_version + 1,
                 updated_at = :updated_at
             WHERE id = 1'
        );
        $statement->execute([
            'updated_at' => $updatedAt,
        ]);

        return (int) $this->getCurrent()['current_version'];
    }
}

