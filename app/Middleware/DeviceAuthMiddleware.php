<?php

declare(strict_types=1);

namespace App\Middleware;

use App\Core\Request;
use App\Core\Response;
use App\Repositories\DeviceRepository;

class DeviceAuthMiddleware
{
    public function __construct(private readonly DeviceRepository $deviceRepository = new DeviceRepository())
    {
    }

    public function handle(Request $request): ?Response
    {
        $token = $request->bearerToken();

        if (!is_string($token) || $token === '') {
            return Response::json([
                'success' => false,
                'message' => 'Device token is required.',
            ], 401);
        }

        $device = $this->deviceRepository->findActiveByToken($token);

        if ($device === null) {
            return Response::json([
                'success' => false,
                'message' => 'Unauthorized device.',
            ], 401);
        }

        $request->setAttribute('authenticated_device', $device);

        return null;
    }
}

