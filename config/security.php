<?php

declare(strict_types=1);

return [
    'app_key' => env('APP_KEY', ''),
    'device_pairing_ttl_minutes' => 10,
    'device_token_delivery_ttl_minutes' => 1440,
    'device_sync_interval_seconds' => 300,
];

