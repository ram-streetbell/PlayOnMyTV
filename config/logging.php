<?php

declare(strict_types=1);

return [
    'channel' => env('LOG_CHANNEL', 'file'),
    'level' => env('LOG_LEVEL', 'debug'),
    'path' => base_path(env('LOG_PATH', 'storage/logs/app.log')),
];

