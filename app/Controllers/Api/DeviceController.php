<?php

declare(strict_types=1);

namespace App\Controllers\Api;

use App\Core\ApiController;
use App\Core\Request;
use App\Services\PairingService;

class DeviceController extends ApiController
{
    public function __construct(private readonly PairingService $pairingService = new PairingService())
    {
    }

    public function startPairing(Request $request)
    {
        $deviceUuid = (string) $request->input('device_uuid', '');

        $result = $this->pairingService->start($deviceUuid, $request->all(), $request->ip());

        return $this->success($result, 201);
    }

    public function pairingStatus(Request $request)
    {
        $deviceUuid = (string) $request->input('device_uuid', '');

        $result = $this->pairingService->status($deviceUuid);

        return $this->success($result);
    }
}
