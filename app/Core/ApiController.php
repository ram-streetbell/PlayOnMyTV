<?php

declare(strict_types=1);

namespace App\Core;

abstract class ApiController extends Controller
{
    protected function success(array $data = [], int $status = 200): Response
    {
        return $this->json([
            'success' => true,
            'data' => $data,
        ], $status);
    }

    protected function error(string $message, int $status = 400, array $errors = []): Response
    {
        return $this->json([
            'success' => false,
            'message' => $message,
            'errors' => $errors,
        ], $status);
    }
}

