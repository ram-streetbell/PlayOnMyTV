<?php

declare(strict_types=1);

namespace App\Services;

use App\Exceptions\HttpException;
use App\Repositories\UserRepository;

class AuthService
{
    public function __construct(private readonly UserRepository $userRepository = new UserRepository())
    {
    }

    public function currentUser(): array
    {
        $user = $this->currentUserOrNull();

        if ($user === null) {
            throw new HttpException('Authentication required.', 401);
        }

        return $user;
    }

    public function currentUserOrNull(): ?array
    {
        $sessionUserId = $_SESSION['user']['id'] ?? $_SESSION['user_id'] ?? null;

        if (!is_numeric($sessionUserId)) {
            return null;
        }

        return $this->userRepository->findActiveById((int) $sessionUserId);
    }
}
