<?php

declare(strict_types=1);

namespace App\Controllers\Api;

use App\Core\ApiController;
use App\Core\Request;
use App\Services\AuthService;
use App\Services\PairingService;

class PairingController extends ApiController
{
    public function __construct(
        private readonly PairingService $pairingService = new PairingService(),
        private readonly AuthService $authService = new AuthService()
    ) {
    }

    public function submitCode(Request $request)
    {
        $user = $this->authService->currentUser();
        $result = $this->pairingService->submitCode(
            (string) $request->input('pairing_code', ''),
            $user,
            $request->ip()
        );

        return $this->success($result);
    }
}
