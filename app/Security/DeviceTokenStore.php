<?php

declare(strict_types=1);

namespace App\Security;

use App\Core\Crypt;
use DateTimeImmutable;

class DeviceTokenStore
{
    public function store(string $deviceUuid, string $token, DateTimeImmutable $expiresAt): void
    {
        $directory = storage_path('temp/device_tokens');

        if (!is_dir($directory)) {
            mkdir($directory, 0777, true);
        }

        $payload = [
            'device_uuid' => $deviceUuid,
            'token' => Crypt::encrypt($token),
            'expires_at' => $expiresAt->format(DATE_ATOM),
        ];

        file_put_contents($this->filePath($deviceUuid), json_encode($payload, JSON_UNESCAPED_SLASHES));
    }

    public function get(string $deviceUuid): ?string
    {
        $path = $this->filePath($deviceUuid);

        if (!file_exists($path)) {
            return null;
        }

        $decoded = json_decode((string) file_get_contents($path), true);

        if (!is_array($decoded) || !isset($decoded['token'], $decoded['expires_at'])) {
            return null;
        }

        if ((new DateTimeImmutable($decoded['expires_at'])) < new DateTimeImmutable()) {
            @unlink($path);
            return null;
        }

        return Crypt::decrypt((string) $decoded['token']);
    }

    private function filePath(string $deviceUuid): string
    {
        return storage_path('temp/device_tokens/' . hash('sha256', $deviceUuid) . '.json');
    }
}

