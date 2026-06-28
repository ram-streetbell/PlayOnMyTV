<?php

declare(strict_types=1);

namespace App\Controllers\Api;

use App\Core\ApiController;
use App\Core\Request;
use App\Services\ManifestService;

class ManifestController extends ApiController
{
    public function __construct(private readonly ManifestService $manifestService = new ManifestService())
    {
    }

    public function show(Request $request)
    {
        $device = $request->attribute('authenticated_device');

        return $this->json($this->manifestService->buildForDevice($device));
    }
}
