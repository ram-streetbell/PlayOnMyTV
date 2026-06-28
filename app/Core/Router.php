<?php

declare(strict_types=1);

namespace App\Core;

use App\Exceptions\HttpException;

class Router
{
    private array $routes = [];

    public function get(string $uri, callable|array|string $action, array $middleware = []): void
    {
        $this->addRoute('GET', $uri, $action, $middleware);
    }

    public function post(string $uri, callable|array|string $action, array $middleware = []): void
    {
        $this->addRoute('POST', $uri, $action, $middleware);
    }

    public function put(string $uri, callable|array|string $action, array $middleware = []): void
    {
        $this->addRoute('PUT', $uri, $action, $middleware);
    }

    public function delete(string $uri, callable|array|string $action, array $middleware = []): void
    {
        $this->addRoute('DELETE', $uri, $action, $middleware);
    }

    public function addRoute(string $method, string $uri, callable|array|string $action, array $middleware = []): void
    {
        $normalizedUri = '/' . trim($uri, '/');
        $this->routes[$method][] = [
            'uri' => $normalizedUri,
            'pattern' => $this->buildRoutePattern($normalizedUri),
            'action' => $action,
            'middleware' => $middleware,
        ];
    }

    public function dispatch(Request $request): Response
    {
        $method = $request->method();
        $uri = $request->path();

        if (!isset($this->routes[$method])) {
            throw new HttpException('Route not found.', 404);
        }
        $route = null;
        $routeParams = [];

        foreach ($this->routes[$method] as $candidateRoute) {
            if (preg_match($candidateRoute['pattern'], $uri, $matches) !== 1) {
                continue;
            }

            $route = $candidateRoute;

            foreach ($matches as $key => $value) {
                if (!is_int($key)) {
                    $routeParams[$key] = $value;
                }
            }

            break;
        }

        if ($route === null) {
            throw new HttpException('Route not found.', 404);
        }

        $request->setRouteParams($routeParams);

        foreach ($route['middleware'] as $middlewareClass) {
            $middleware = new $middlewareClass();
            $result = $middleware->handle($request);

            if ($result instanceof Response) {
                return $result;
            }
        }

        $action = $route['action'];

        if (is_array($action)) {
            [$controllerClass, $methodName] = $action;
            $controller = new $controllerClass();
            $result = $controller->{$methodName}($request);
        } elseif (is_string($action) && class_exists($action)) {
            $controller = new $action();
            $result = $controller($request);
        } else {
            $result = $action($request);
        }

        if ($result instanceof Response) {
            return $result;
        }

        if (is_array($result)) {
            return Response::json($result);
        }

        if (is_string($result)) {
            return Response::html($result);
        }

        return Response::html('');
    }

    private function buildRoutePattern(string $uri): string
    {
        $pattern = preg_replace('/\{([a-zA-Z_][a-zA-Z0-9_]*)\}/', '(?P<$1>[^/]+)', $uri);

        return '#^' . $pattern . '$#';
    }
}
