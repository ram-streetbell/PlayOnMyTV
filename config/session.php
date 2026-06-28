<?php

declare(strict_types=1);

return [
    'name' => env('SESSION_NAME', 'playonmytv_session'),
    'lifetime' => env('SESSION_LIFETIME', 120, 'int'),
    'csrf_key' => env('CSRF_TOKEN_KEY', '_csrf_token'),
];

