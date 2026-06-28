<?php

declare(strict_types=1);

namespace App\Core;

class Application
{
    public function __construct(
        private readonly string $basePath,
        private readonly Router $router,
        private readonly SessionManager $sessionManager
    ) {
    }

    public function run(): void
    {
        $request = Request::capture();
        $response = $this->router->dispatch($request);
        $response->send();
        $this->sessionManager->terminateRequest();
    }

    public function basePath(): string
    {
        return $this->basePath;
    }
}

