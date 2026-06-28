<?php

declare(strict_types=1);

namespace App\Middleware;

use App\Core\Request;
use App\Core\Response;

class CsrfMiddleware
{
    public function handle(Request $request): ?Response
    {
        if (!in_array($request->method(), ['POST', 'PUT', 'DELETE'], true)) {
            return null;
        }

        $sessionToken = $_SESSION[config('session.csrf_key', '_csrf_token')] ?? null;
        $requestToken = $request->input('_token', $request->header('X-CSRF-TOKEN'));

        if (!is_string($sessionToken) || !is_string($requestToken) || !hash_equals($sessionToken, $requestToken)) {
            if ($request->expectsJson()) {
                return Response::json([
                    'success' => false,
                    'message' => 'Invalid CSRF token.',
                ], 419);
            }

            return Response::html('Invalid CSRF token.', 419);
        }

        return null;
    }
}
