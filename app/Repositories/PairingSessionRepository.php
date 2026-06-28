<?php

declare(strict_types=1);

namespace App\Repositories;

class PairingSessionRepository extends BaseRepository
{
    public function createPending(int $deviceId, string $pairingCode, string $expiresAt, string $createdAt): int
    {
        $statement = $this->connection->prepare(
            'INSERT INTO pairing_sessions (
                device_id,
                pairing_code,
                status,
                expires_at,
                paired_by_user_id,
                paired_at,
                created_at
             ) VALUES (
                :device_id,
                :pairing_code,
                :status,
                :expires_at,
                NULL,
                NULL,
                :created_at
             )'
        );
        $statement->execute([
            'device_id' => $deviceId,
            'pairing_code' => $pairingCode,
            'status' => 'pending',
            'expires_at' => $expiresAt,
            'created_at' => $createdAt,
        ]);

        return (int) $this->connection->lastInsertId();
    }

    public function expirePendingForDevice(int $deviceId, string $status): void
    {
        $statement = $this->connection->prepare(
            'UPDATE pairing_sessions
             SET status = :status
             WHERE device_id = :device_id AND status = :pending_status'
        );
        $statement->execute([
            'status' => $status,
            'device_id' => $deviceId,
            'pending_status' => 'pending',
        ]);
    }

    public function findValidPendingByCode(string $pairingCode, string $now): ?array
    {
        $statement = $this->connection->prepare(
            'SELECT ps.*, d.device_uuid, d.device_name, d.business_id
             FROM pairing_sessions ps
             INNER JOIN devices d ON d.id = ps.device_id
             WHERE ps.pairing_code = :pairing_code
               AND ps.status = :status
               AND ps.expires_at > :now
             ORDER BY ps.id DESC
             LIMIT 1'
        );
        $statement->execute([
            'pairing_code' => $pairingCode,
            'status' => 'pending',
            'now' => $now,
        ]);

        $session = $statement->fetch();

        return is_array($session) ? $session : null;
    }

    public function markPaired(int $sessionId, int $pairedByUserId, string $pairedAt): void
    {
        $statement = $this->connection->prepare(
            'UPDATE pairing_sessions
             SET status = :status,
                 paired_by_user_id = :paired_by_user_id,
                 paired_at = :paired_at
             WHERE id = :id'
        );
        $statement->execute([
            'status' => 'paired',
            'paired_by_user_id' => $pairedByUserId,
            'paired_at' => $pairedAt,
            'id' => $sessionId,
        ]);
    }
}
