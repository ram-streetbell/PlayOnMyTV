<?php

declare(strict_types=1);

namespace App\Middleware;

use App\Core\Request;
use App\Core\Response;

class AuthMiddleware
{
    public function handle(Request $request): ?Response
    {
        if (!isset($_SESSION['user'])) {
            if ($request->expectsJson()) {
                return Response::json([
                    'success' => false,
                    'message' => 'Authentication required.',
                ], 401);
            }

            return Response::redirect('/login');
        }

        return null;
    }
}
