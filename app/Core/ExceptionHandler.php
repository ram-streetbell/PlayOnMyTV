<?php

declare(strict_types=1);

namespace App\Core;

use Throwable;

class ExceptionHandler
{
    public static function register(): void
    {
        set_exception_handler([self::class, 'handle']);
    }

    public static function handle(Throwable $exception): void
    {
        Logger::error($exception->getMessage(), [
            'exception' => $exception::class,
            'file' => $exception->getFile(),
            'line' => $exception->getLine(),
        ]);

        $status = method_exists($exception, 'getStatusCode') ? $exception->getStatusCode() : 500;

        if (self::expectsJson()) {
            Response::json([
                'success' => false,
                'message' => $status === 500 ? 'Internal server error.' : $exception->getMessage(),
                'errors' => method_exists($exception, 'getErrors') ? $exception->getErrors() : [],
            ], $status)->send();
            return;
        }

        Response::html(View::render('errors/generic', [
            'status' => $status,
            'message' => $status === 500 ? 'Something went wrong.' : $exception->getMessage(),
        ], 'layouts/guest'), $status)->send();
    }

    private static function expectsJson(): bool
    {
        $accept = $_SERVER['HTTP_ACCEPT'] ?? '';
        $uri = $_SERVER['REQUEST_URI'] ?? '';

        return str_contains($accept, 'application/json') || str_starts_with($uri, '/api/');
    }
}
