<?php

declare(strict_types=1);

namespace App\Core;

abstract class Controller
{
    protected function view(string $template, array $data = [], string $layout = 'layouts/app'): Response
    {
        return \view($template, $data, $layout);
    }

    protected function json(array $data = [], int $status = 200): Response
    {
        return \json($data, $status);
    }

    protected function redirect(string $url): Response
    {
        return \redirect($url);
    }
}

