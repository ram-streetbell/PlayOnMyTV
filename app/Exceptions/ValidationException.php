<?php

declare(strict_types=1);

namespace App\Exceptions;

class ValidationException extends HttpException
{
    public function __construct(
        string $message = 'Validation failed.',
        private readonly array $errors = [],
        int $statusCode = 422
    ) {
        parent::__construct($message, $statusCode);
    }

    public function getErrors(): array
    {
        return $this->errors;
    }
}

