<?php

declare(strict_types=1);

namespace App\Services;

use App\Exceptions\HttpException;
use App\Repositories\RateLimitRepository;
use DateTimeImmutable;

class RateLimitService
{
    public function __construct(private readonly RateLimitRepository $repository = new RateLimitRepository())
    {
    }

    public function enforce(string $actionKey, string $identifier, int $maxAttempts, int $windowSeconds): void
    {
        $now = new DateTimeImmutable();
        $window = $this->repository->findWindow($actionKey, $identifier);

        if ($window === null) {
            $this->repository->createWindow(
                $actionKey,
                $identifier,
                1,
                $now->format('Y-m-d H:i:s'),
                $now->format('Y-m-d H:i:s')
            );

            return;
        }

        $windowStartedAt = new DateTimeImmutable($window['window_started_at']);
        $diff = $now->getTimestamp() - $windowStartedAt->getTimestamp();

        if ($diff >= $windowSeconds) {
            $this->repository->updateWindow(
                (int) $window['id'],
                1,
                $now->format('Y-m-d H:i:s'),
                $now->format('Y-m-d H:i:s')
            );

            return;
        }

        $attemptCount = (int) $window['attempt_count'] + 1;

        if ($attemptCount > $maxAttempts) {
            throw new HttpException('Too many attempts. Please try again later.', 429);
        }

        $this->repository->updateWindow(
            (int) $window['id'],
            $attemptCount,
            $windowStartedAt->format('Y-m-d H:i:s'),
            $now->format('Y-m-d H:i:s')
        );
    }
}

