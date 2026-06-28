<?php

declare(strict_types=1);

namespace App\Controllers\Web;

use App\Core\Controller;
use App\Core\Request;
use App\Services\AuthService;
use App\Services\DeviceService;

class DeviceController extends Controller
{
    public function __construct(
        private readonly DeviceService $deviceService = new DeviceService(),
        private readonly AuthService $authService = new AuthService()
    ) {
    }

    public function index(Request $request)
    {
        $currentUser = $this->authService->currentUserOrNull();
        $devices = $currentUser !== null
            ? $this->deviceService->listForBusiness((int) $currentUser['business_id'])
            : [];

        return $this->view('devices/index', [
            'pageTitle' => 'Devices',
            'devices' => $devices,
            'currentUser' => $currentUser,
        ]);
    }
}
