<?php

declare(strict_types=1);

namespace App\Repositories;

class RateLimitRepository extends BaseRepository
{
    public function findWindow(string $actionKey, string $identifier): ?array
    {
        $statement = $this->connection->prepare(
            'SELECT *
             FROM rate_limit_attempts
             WHERE action_key = :action_key
               AND identifier = :identifier
             LIMIT 1'
        );
        $statement->execute([
            'action_key' => $actionKey,
            'identifier' => $identifier,
        ]);

        $row = $statement->fetch();

        return is_array($row) ? $row : null;
    }

    public function createWindow(string $actionKey, string $identifier, int $attemptCount, string $windowStartedAt, string $updatedAt): void
    {
        $statement = $this->connection->prepare(
            'INSERT INTO rate_limit_attempts (
                action_key,
                identifier,
                attempt_count,
                window_started_at,
                created_at,
                updated_at
             ) VALUES (
                :action_key,
                :identifier,
                :attempt_count,
                :window_started_at,
                :created_at,
                :updated_at
             )'
        );
        $statement->execute([
            'action_key' => $actionKey,
            'identifier' => $identifier,
            'attempt_count' => $attemptCount,
            'window_started_at' => $windowStartedAt,
            'created_at' => $updatedAt,
            'updated_at' => $updatedAt,
        ]);
    }

    public function updateWindow(int $id, int $attemptCount, string $windowStartedAt, string $updatedAt): void
    {
        $statement = $this->connection->prepare(
            'UPDATE rate_limit_attempts
             SET attempt_count = :attempt_count,
                 window_started_at = :window_started_at,
                 updated_at = :updated_at
             WHERE id = :id'
        );
        $statement->execute([
            'attempt_count' => $attemptCount,
            'window_started_at' => $windowStartedAt,
            'updated_at' => $updatedAt,
            'id' => $id,
        ]);
    }
}

