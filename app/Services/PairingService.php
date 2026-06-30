<?php

declare(strict_types=1);

namespace App\Services;

use App\Core\Database;
use App\Exceptions\HttpException;
use App\Exceptions\ValidationException;
use App\Repositories\DeviceRepository;
use App\Repositories\PairingSessionRepository;
use App\Security\DeviceTokenStore;
use DateInterval;
use DateTimeImmutable;
use PDO;
use Throwable;

class PairingService
{
    public function __construct(
        private readonly DeviceRepository $deviceRepository = new DeviceRepository(),
        private readonly PairingSessionRepository $pairingSessionRepository = new PairingSessionRepository(),
        private readonly RateLimitService $rateLimitService = new RateLimitService(),
        private readonly DeviceTokenStore $deviceTokenStore = new DeviceTokenStore()
    ) {
    }

    public function start(string $deviceUuid, array $payload, string $ipAddress): array
    {
        $this->assertValidUuid($deviceUuid);
        $this->rateLimitService->enforce('pairing_start', $deviceUuid . '|' . $ipAddress, 30, 3600);

        $now = new DateTimeImmutable();
        $device = $this->deviceRepository->findByUuid($deviceUuid);
        error_log('DEVICE FOUND: ' . json_encode($device));
        $deviceName = $this->sanitizeDeviceName((string) ($payload['device_name'] ?? 'PlayOnMyTV Screen'));
        $appVersion = $this->sanitizeVersion((string) ($payload['app_version'] ?? 'unknown'));
        $firmwareVersion = $this->sanitizeOptional((string) ($payload['firmware_version'] ?? ''), 100);
        $timezone = $this->sanitizeOptional((string) ($payload['timezone'] ?? ''), 64);
        $screenResolution = $this->sanitizeOptional((string) ($payload['screen_resolution'] ?? ''), 50);

        if ($device !== null && $device['status'] === 'active' && !empty($device['device_token_hash'])) {
            throw new HttpException('Device is already paired.', 409);
        }

        if ($device !== null && !empty($device['pairing_code']) && !empty($device['pairing_code_expires_at'])) {
            $expiresAt = new DateTimeImmutable($device['pairing_code_expires_at']);

            if ($expiresAt > $now) {
                return [
                    'device_uuid' => $device['device_uuid'],
                    'pairing_code' => $device['pairing_code'],
                    'expires_at' => $expiresAt->format(DATE_ATOM),
                ];
            }
        }

        $pairingCode = $this->generateUniquePairingCode($now);
        $expiresAt = $now->add(new DateInterval('PT' . (int) config('security.device_pairing_ttl_minutes', 10) . 'M'));
        $connection = Database::connection();
        $connection->beginTransaction();

        try {
            if ($device === null) {
                $deviceId = $this->deviceRepository->createPending([
                    'device_uuid' => $deviceUuid,
                    'device_name' => $deviceName,
                    'platform' => 'android-tv',
                    'app_version' => $appVersion,
                    'firmware_version' => $firmwareVersion,
                    'pairing_code' => $pairingCode,
                    'pairing_code_expires_at' => $expiresAt->format('Y-m-d H:i:s'),
                    'status' => 'pending_pairing',
                    'timezone' => $timezone,
                    'screen_resolution' => $screenResolution,
                    'created_at' => $now->format('Y-m-d H:i:s'),
                    'updated_at' => $now->format('Y-m-d H:i:s'),
                ]);
            } else {
                $deviceId = (int) $device['id'];
                $this->deviceRepository->updatePairingState($deviceId, [
                    'device_name' => $deviceName,
                    'app_version' => $appVersion,
                    'firmware_version' => $firmwareVersion,
                    'pairing_code' => $pairingCode,
                    'pairing_code_expires_at' => $expiresAt->format('Y-m-d H:i:s'),
                    'status' => 'pending_pairing',
                    'timezone' => $timezone,
                    'screen_resolution' => $screenResolution,
                    'updated_at' => $now->format('Y-m-d H:i:s'),
                ]);
                $this->pairingSessionRepository->expirePendingForDevice($deviceId, 'expired');
            }

            $this->pairingSessionRepository->createPending(
                $deviceId,
                $pairingCode,
                $expiresAt->format('Y-m-d H:i:s'),
                $now->format('Y-m-d H:i:s')
            );
            $connection->commit();
        } catch (Throwable $throwable) {
            if ($connection->inTransaction()) {
                $connection->rollBack();
            }

            throw $throwable;
        }

        return [
            'device_uuid' => $deviceUuid,
            'pairing_code' => $pairingCode,
            'expires_at' => $expiresAt->format(DATE_ATOM),
        ];
    }

    public function submitCode(string $pairingCode, array $user, string $ipAddress): array
    {
        $normalizedCode = strtoupper(trim($pairingCode));

        if (!preg_match('/^[A-Z0-9]{6}$/', $normalizedCode)) {
            throw new ValidationException('Invalid pairing code.', [
                'pairing_code' => ['Pairing code must be 6 uppercase letters or numbers.'],
            ]);
        }

        $this->rateLimitService->enforce('pairing_submit', $user['id'] . '|' . $ipAddress, 10, 600);

        $now = new DateTimeImmutable();
        $session = $this->pairingSessionRepository->findValidPendingByCode($normalizedCode, $now->format('Y-m-d H:i:s'));

        if ($session === null) {
            throw new HttpException('Pairing code is invalid or expired.', 404);
        }

        $deviceToken = bin2hex(random_bytes(32));
        $connection = Database::connection();
        $connection->beginTransaction();

        try {
            $deviceName = trim((string) $session['device_name']) !== ''
                ? (string) $session['device_name']
                : 'Screen ' . substr((string) $session['device_uuid'], 0, 6);

            $this->deviceRepository->markPaired((int) $session['device_id'], [
                'business_id' => (int) $user['business_id'],
                'device_name' => $deviceName,
                'device_token_hash' => hash('sha256', $deviceToken),
                'status' => 'active',
                'updated_at' => $now->format('Y-m-d H:i:s'),
            ]);

            $this->pairingSessionRepository->markPaired(
                (int) $session['id'],
                (int) $user['id'],
                $now->format('Y-m-d H:i:s')
            );

            $this->deviceTokenStore->store(
                (string) $session['device_uuid'],
                $deviceToken,
                $now->add(new DateInterval('PT' . (int) config('security.device_token_delivery_ttl_minutes', 1440) . 'M'))
            );

            $connection->commit();
        } catch (Throwable $throwable) {
            if ($connection->inTransaction()) {
                $connection->rollBack();
            }

            throw $throwable;
        }

        return [
            'message' => 'Device paired successfully.',
            'device_uuid' => $session['device_uuid'],
            'device_name' => $deviceName,
        ];
    }

    public function status(string $deviceUuid): array
    {
        $this->assertValidUuid($deviceUuid);
        $device = $this->deviceRepository->findByUuid($deviceUuid);

        if ($device === null) {
            throw new HttpException('Device not found.', 404);
        }

        if ($device['status'] === 'active' && !empty($device['device_token_hash'])) {
            $deviceToken = $this->deviceTokenStore->get($deviceUuid);

            if ($deviceToken === null) {
                throw new HttpException('Device token handoff expired. Re-pair the device.', 410);
            }

            return [
                'waiting' => false,
                'device_token' => $deviceToken,
                'device_name' => $device['device_name'],
                'sync_interval' => (int) config('security.device_sync_interval_seconds', 300),
            ];
        }

        return [
            'waiting' => true,
        ];
    }

    private function assertValidUuid(string $deviceUuid): void
    {
        if (!preg_match('/^[0-9a-fA-F-]{36}$/', $deviceUuid)) {
            throw new ValidationException('Invalid device UUID.', [
                'device_uuid' => ['A valid device UUID is required.'],
            ]);
        }
    }

    private function sanitizeDeviceName(string $deviceName): string
    {
        $deviceName = trim($deviceName);

        if ($deviceName === '') {
            return 'PlayOnMyTV Screen';
        }

        return substr($deviceName, 0, 150);
    }

    private function sanitizeVersion(string $version): string
    {
        return substr(trim($version) ?: 'unknown', 0, 50);
    }

    private function sanitizeOptional(string $value, int $maxLength): ?string
    {
        $value = trim($value);

        return $value === '' ? null : substr($value, 0, $maxLength);
    }

    private function generateUniquePairingCode(DateTimeImmutable $now): string
    {
        $characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';

        do {
            $code = '';

            for ($i = 0; $i < 6; $i++) {
                $code .= $characters[random_int(0, strlen($characters) - 1)];
            }
        } while ($this->deviceRepository->pairingCodeInUse($code, $now));

        return $code;
    }
}
