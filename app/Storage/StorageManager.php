<?php

declare(strict_types=1);

namespace App\Storage;

use App\Exceptions\StorageException;
use App\Storage\Cloudinary\CloudinaryStorage;
use App\Storage\Contracts\MediaStorageInterface;

class StorageManager
{
    private ?MediaStorageInterface $provider = null;

    public function __construct(?MediaStorageInterface $provider = null)
    {
        $this->provider = $provider;
    }

    public function provider(): MediaStorageInterface
    {
        if ($this->provider instanceof MediaStorageInterface) {
            return $this->provider;
        }

        $driver = config('storage.default', 'cloudinary');

        $this->provider = match ($driver) {
            'cloudinary' => new CloudinaryStorage(),
            default => throw new StorageException('Unsupported media storage provider [' . $driver . '].'),
        };

        return $this->provider;
    }
}
