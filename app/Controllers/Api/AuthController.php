<?php

declare(strict_types=1);

namespace App\Controllers\Api;

use App\Core\ApiController;
use App\Core\Request;

class AuthController extends ApiController
{
    public function __invoke(Request $request)
    {
        return $this->success([
            'module' => 'auth',
            'status' => 'scaffold',
        ]);
    }
}

