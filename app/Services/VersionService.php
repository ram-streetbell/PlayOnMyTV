<?php

declare(strict_types=1);

namespace App\Services;

use App\Repositories\ManifestVersionRepository;
use DateTimeImmutable;

class VersionService
{
    public function __construct(
        private readonly ManifestVersionRepository $manifestVersionRepository = new ManifestVersionRepository()
    ) {
    }

    public function current(): array
    {
        $this->manifestVersionRepository->ensureInitialized($this->now());

        return $this->manifestVersionRepository->getCurrent();
    }

    public function increment(): int
    {
        return $this->manifestVersionRepository->increment($this->now());
    }

    private function now(): string
    {
        return (new DateTimeImmutable())->format('Y-m-d H:i:s');
    }
}

