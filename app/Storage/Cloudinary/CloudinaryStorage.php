<?php

declare(strict_types=1);

namespace App\Storage\Cloudinary;

use App\Exceptions\StorageException;
use App\Storage\Contracts\MediaStorageInterface;
use Cloudinary\Api\ApiResponse;
use Cloudinary\Api\Upload\UploadApi;
use Cloudinary\Cloudinary;
use Throwable;

class CloudinaryStorage implements MediaStorageInterface
{
    private Cloudinary $cloudinary;
    private UploadApi $uploadApi;

    public function __construct()
    {
        $config = config('storage.cloudinary');

        if (
            empty($config['cloud_name']) ||
            empty($config['api_key']) ||
            empty($config['api_secret'])
        ) {
            throw new StorageException('Cloudinary configuration is incomplete.');
        }

        $this->cloudinary = new Cloudinary([
            'cloud' => [
                'cloud_name' => $config['cloud_name'],
                'api_key' => $config['api_key'],
                'api_secret' => $config['api_secret'],
            ],
            'url' => [
                'secure' => true,
            ],
        ]);

        $this->uploadApi = $this->cloudinary->uploadApi();
    }

    public function upload(string $localPath, array $options = []): array
    {
        try {
            $result = $this->uploadApi->upload($localPath, $options);
            $result = $this->normalizeResponse($result);
        } catch (Throwable $throwable) {
            throw new StorageException(
                'Cloudinary upload failed: ' . $throwable->getMessage(),
                0,
                $throwable
            );
        }

        return $this->normalizeUploadResult($result, $options);
    }

    public function delete(string $storageKey, array $options = []): bool
    {
        try {
            $result = $this->uploadApi->destroy(
                $storageKey,
                $options + ['invalidate' => true]
            );

            $result = $this->normalizeResponse($result);
        } catch (Throwable $throwable) {
            throw new StorageException(
                'Cloudinary delete failed: ' . $throwable->getMessage(),
                0,
                $throwable
            );
        }

        return in_array(($result['result'] ?? null), ['ok', 'not found'], true);
    }

    public function replace(string $storageKey, string $localPath, array $options = []): array
    {
        $options['public_id'] = $storageKey;
        $options['overwrite'] = true;
        $options['invalidate'] = true;

        return $this->upload($localPath, $options);
    }

    public function getDeliveryUrl(string $storageKey, array $options = []): string
    {
        $resourceType = $options['resource_type'] ?? 'image';
        $transformation = $options['transformation'] ?? null;
        $format = $options['format'] ?? null;

        return $this->buildUrl(
            $storageKey,
            $resourceType,
            $transformation,
            $format
        );
    }

    public function getSignedDownloadUrl(
        string $storageKey,
        int $ttlSeconds = 900,
        array $options = []
    ): string {
        return $this->getDeliveryUrl($storageKey, $options);
    }

    /**
     * Convert Cloudinary ApiResponse into array.
     */
    private function normalizeResponse(mixed $result): array
    {
        if (is_array($result)) {
            return $result;
        }

        if ($result instanceof ApiResponse) {

            if (method_exists($result, 'getArrayCopy')) {
                return $result->getArrayCopy();
            }

            if (method_exists($result, 'toArray')) {
                return $result->toArray();
            }

            if ($result instanceof \JsonSerializable) {
                return $result->jsonSerialize();
            }

            return json_decode(json_encode($result), true);
        }

        return (array)$result;
    }

    private function normalizeUploadResult(array $result, array $options): array
    {
        $resourceType = (string)(
            $result['resource_type']
            ?? $options['resource_type']
            ?? 'image'
        );

        $publicId = (string)($result['public_id'] ?? '');
        $format = (string)($result['format'] ?? '');

        return [
            'storage_key' => $publicId,

            'storage_url' => (string)(
                $result['secure_url']
                ?? $this->buildUrl(
                    $publicId,
                    $resourceType,
                    null,
                    $format
                )
            ),

            'thumbnail_url' => $resourceType === 'video'
                ? $this->buildUrl(
                    $publicId,
                    'video',
                    'so_0',
                    'jpg'
                )
                : $this->buildUrl(
                    $publicId,
                    'image',
                    'c_fill,f_auto,g_auto,h_240,q_auto,w_360',
                    $format
                ),

            'width' => isset($result['width'])
                ? (int)$result['width']
                : null,

            'height' => isset($result['height'])
                ? (int)$result['height']
                : null,

            'duration_seconds' => isset($result['duration'])
                ? (int)ceil((float)$result['duration'])
                : null,

            'bytes' => isset($result['bytes'])
                ? (int)$result['bytes']
                : null,

            'format' => $format,

            'resource_type' => $resourceType,

            'raw' => $result,
        ];
    }

    private function buildUrl(
        string $storageKey,
        string $resourceType,
        ?string $transformation,
        ?string $format
    ): string {

        $cloudName = (string)config(
            'storage.cloudinary.cloud_name',
            ''
        );

        $encodedPath = implode(
            '/',
            array_map(
                'rawurlencode',
                explode('/', $storageKey)
            )
        );

        $base = sprintf(
            'https://res.cloudinary.com/%s/%s/upload',
            rawurlencode($cloudName),
            $resourceType
        );

        if ($transformation) {
            $base .= '/' . $transformation;
        }

        if ($format) {
            return $base . '/' . $encodedPath . '.' . rawurlencode($format);
        }

        return $base . '/' . $encodedPath;
    }
}