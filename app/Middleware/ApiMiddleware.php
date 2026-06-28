<?php

declare(strict_types=1);

namespace App\Middleware;

use App\Core\Request;
use App\Core\Response;

class ApiMiddleware
{
    public function handle(Request $request): ?Response
    {
        if (!$request->expectsJson()) {
            return Response::json([
                'success' => false,
                'message' => 'API requests must use JSON headers.',
            ], 406);
        }

        return null;
    }
}

