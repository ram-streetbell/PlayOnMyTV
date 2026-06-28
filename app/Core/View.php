<?php

declare(strict_types=1);

namespace App\Core;

use App\Exceptions\ViewException;

class View
{
    public static function render(string $template, array $data = [], string $layout = 'layouts/app'): string
    {
        $content = self::renderFile($template, $data);

        if ($layout === '') {
            return $content;
        }

        return self::renderFile($layout, array_merge($data, ['content' => $content]));
    }

    private static function renderFile(string $template, array $data): string
    {
        $path = base_path('app/Views/' . str_replace('.', '/', $template) . '.php');

        if (!file_exists($path)) {
            throw new ViewException(sprintf('View [%s] not found.', $template));
        }

        extract($data, EXTR_SKIP);

        ob_start();
        require $path;

        return (string) ob_get_clean();
    }
}

