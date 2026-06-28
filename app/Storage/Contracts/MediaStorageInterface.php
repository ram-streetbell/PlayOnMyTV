<?php

declare(strict_types=1);

namespace App\Storage\Contracts;

interface MediaStorageInterface
{
    public function upload(string $localPath, array $options = []): array;

    public function delete(string $storageKey, array $options = []): bool;

    public function replace(string $storageKey, string $localPath, array $options = []): array;

    public function getDeliveryUrl(string $storageKey, array $options = []): string;

    public function getSignedDownloadUrl(string $storageKey, int $ttlSeconds = 900, array $options = []): string;
}
