<?php

declare(strict_types=1);

namespace App\Core;

class Request
{
    private ?array $jsonPayload = null;
    private array $routeParams = [];
    private array $attributes = [];

    public function __construct(
        private readonly array $get,
        private readonly array $post,
        private readonly array $server,
        private readonly array $files,
        private readonly array $cookies,
        private readonly array $headers,
        private readonly string $rawBody
    ) {
    }

    public static function capture(): self
    {
        return new self(
            $_GET,
            $_POST,
            $_SERVER,
            $_FILES,
            $_COOKIE,
            function_exists('getallheaders') ? getallheaders() : [],
            file_get_contents('php://input') ?: ''
        );
    }

    public function method(): string
    {
        return strtoupper($this->server['REQUEST_METHOD'] ?? 'GET');
    }

    public function path(): string
    {
        $uri = $this->server['REQUEST_URI'] ?? '/';
        $path = parse_url($uri, PHP_URL_PATH) ?: '/';

        return '/' . trim($path, '/') ?: '/';
    }

    public function input(string $key, mixed $default = null): mixed
    {
        $payload = $this->json();

        return $this->post[$key] ?? $this->get[$key] ?? $payload[$key] ?? $default;
    }

    public function all(): array
    {
        return array_merge($this->get, $this->post, $this->json());
    }

    public function query(string $key, mixed $default = null): mixed
    {
        return $this->get[$key] ?? $default;
    }

    public function file(string $key): mixed
    {
        return $this->files[$key] ?? null;
    }

    public function header(string $key, mixed $default = null): mixed
    {
        foreach ($this->headers as $headerKey => $value) {
            if (strcasecmp($headerKey, $key) === 0) {
                return $value;
            }
        }

        return $default;
    }

    public function bearerToken(): ?string
    {
        $header = $this->header('Authorization');

        if (!is_string($header) || !str_starts_with($header, 'Bearer ')) {
            return null;
        }

        return substr($header, 7);
    }

    public function expectsJson(): bool
    {
        $accept = (string) $this->header('Accept', '');
        $contentType = (string) $this->header('Content-Type', '');

        return str_contains($accept, 'application/json') || str_contains($contentType, 'application/json');
    }

    public function json(): array
    {
        if (is_array($this->jsonPayload)) {
            return $this->jsonPayload;
        }

        $decoded = json_decode($this->rawBody, true);

        $this->jsonPayload = is_array($decoded) ? $decoded : [];

        return $this->jsonPayload;
    }

    public function server(string $key, mixed $default = null): mixed
    {
        return $this->server[$key] ?? $default;
    }

    public function ip(): string
    {
        return (string) ($this->server['REMOTE_ADDR'] ?? '0.0.0.0');
    }

    public function setRouteParams(array $routeParams): void
    {
        $this->routeParams = $routeParams;
    }

    public function route(string $key, mixed $default = null): mixed
    {
        return $this->routeParams[$key] ?? $default;
    }

    public function setAttribute(string $key, mixed $value): void
    {
        $this->attributes[$key] = $value;
    }

    public function attribute(string $key, mixed $default = null): mixed
    {
        return $this->attributes[$key] ?? $default;
    }
}
