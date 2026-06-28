<?php

declare(strict_types=1);

namespace App\Services;

use App\Repositories\DeviceRepository;

class DeviceService
{
    public function __construct(private readonly DeviceRepository $deviceRepository = new DeviceRepository())
    {
    }

    public function listForBusiness(int $businessId): array
    {
        return $this->deviceRepository->listByBusinessId($businessId);
    }
}
