<?php

declare(strict_types=1);

namespace App\Repositories;

use DateTimeImmutable;

class DeviceRepository extends BaseRepository
{
    public function findActiveByToken(string $token): ?array
    {
        $statement = $this->connection->prepare(
            'SELECT *
             FROM devices
             WHERE device_token_hash = :device_token_hash
               AND status = :status
             LIMIT 1'
        );
        $statement->execute([
            'device_token_hash' => hash('sha256', $token),
            'status' => 'active',
        ]);

        $device = $statement->fetch();

        return is_array($device) ? $device : null;
    }

    public function findByUuid(string $deviceUuid): ?array
    {
        $statement = $this->connection->prepare('SELECT * FROM devices WHERE device_uuid = :device_uuid LIMIT 1');
        $statement->execute(['device_uuid' => $deviceUuid]);

        $device = $statement->fetch();

        return is_array($device) ? $device : null;
    }

    public function createPending(array $attributes): int
    {
        $statement = $this->connection->prepare(
            'INSERT INTO devices (
                business_id,
                device_uuid,
                device_name,
                platform,
                app_version,
                firmware_version,
                pairing_code,
                pairing_code_expires_at,
                status,
                timezone,
                screen_resolution,
                created_at,
                updated_at
            ) VALUES (
                NULL,
                :device_uuid,
                :device_name,
                :platform,
                :app_version,
                :firmware_version,
                :pairing_code,
                :pairing_code_expires_at,
                :status,
                :timezone,
                :screen_resolution,
                :created_at,
                :updated_at
            )'
        );

        $statement->execute($attributes);

        return (int) $this->connection->lastInsertId();
    }

    public function updatePairingState(int $deviceId, array $attributes): void
    {
        $statement = $this->connection->prepare(
            'UPDATE devices
             SET device_name = :device_name,
                 app_version = :app_version,
                 firmware_version = :firmware_version,
                 pairing_code = :pairing_code,
                 pairing_code_expires_at = :pairing_code_expires_at,
                 status = :status,
                 timezone = :timezone,
                 screen_resolution = :screen_resolution,
                 updated_at = :updated_at
             WHERE id = :id'
        );

        $statement->execute($attributes + ['id' => $deviceId]);
    }

    public function markPaired(int $deviceId, array $attributes): void
    {
        $statement = $this->connection->prepare(
            'UPDATE devices
             SET business_id = :business_id,
                 device_name = :device_name,
                 device_token_hash = :device_token_hash,
                 pairing_code = NULL,
                 pairing_code_expires_at = NULL,
                 status = :status,
                 updated_at = :updated_at
             WHERE id = :id'
        );

        $statement->execute($attributes + ['id' => $deviceId]);
    }

    public function listByBusinessId(int $businessId): array
    {
        $statement = $this->connection->prepare(
            'SELECT d.id,
                    d.device_uuid,
                    d.device_name,
                    d.status,
                    d.app_version,
                    d.last_seen_at,
                    d.last_sync_at,
                    d.pairing_code,
                    d.pairing_code_expires_at
             FROM devices d
             WHERE d.business_id = :business_id
             ORDER BY d.created_at DESC'
        );
        $statement->execute([
            'business_id' => $businessId,
        ]);

        return $statement->fetchAll() ?: [];
    }

    public function pairingCodeInUse(string $pairingCode, DateTimeImmutable $now): bool
    {
        $statement = $this->connection->prepare(
            'SELECT id
             FROM devices
             WHERE pairing_code = :pairing_code
               AND pairing_code_expires_at > :now
             LIMIT 1'
        );
        $statement->execute([
            'pairing_code' => $pairingCode,
            'now' => $now->format('Y-m-d H:i:s'),
        ]);

        return (bool) $statement->fetchColumn();
    }
}
