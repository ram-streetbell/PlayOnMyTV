<?php

declare(strict_types=1);

return [
    'default' => env('MEDIA_STORAGE_PROVIDER', 'cloudinary'),
    'cloudinary' => [
        'cloud_name' => env('CLOUDINARY_CLOUD_NAME', ''),
        'api_key' => env('CLOUDINARY_API_KEY', ''),
        'api_secret' => env('CLOUDINARY_API_SECRET', ''),
        'folder' => env('CLOUDINARY_FOLDER', 'playonmytv/media'),
    ],
];

