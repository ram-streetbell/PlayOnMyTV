<?php

declare(strict_types=1);

return [
    'max_upload_size_mb' => env('MEDIA_MAX_UPLOAD_SIZE_MB', 100, 'int'),
    'cloudinary_folder' => env('CLOUDINARY_FOLDER', 'playonmytv/media'),
    'pagination' => [
        'per_page' => 12,
    ],
    'allowed_extensions' => [
        'image' => ['jpg', 'jpeg', 'png', 'webp'],
        'video' => ['mp4', 'webm', 'mov'],
    ],
    'allowed_mime_types' => [
        'jpg' => ['image/jpeg'],
        'jpeg' => ['image/jpeg'],
        'png' => ['image/png'],
        'webp' => ['image/webp'],
        'mp4' => ['video/mp4'],
        'webm' => ['video/webm'],
        'mov' => ['video/quicktime'],
    ],
];

