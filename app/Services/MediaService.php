<?php

declare(strict_types=1);

namespace App\Services;

use App\Core\Database;
use App\Core\Logger;
use App\Exceptions\HttpException;
use App\Exceptions\ValidationException;
use App\Repositories\MediaRepository;
use App\Repositories\SyncChangeRepository;
use App\Storage\StorageManager;
use DateTimeImmutable;
use Throwable;

class MediaService
{
    public function __construct(
        private readonly MediaRepository $mediaRepository = new MediaRepository(),
        private readonly SyncChangeRepository $syncChangeRepository = new SyncChangeRepository(),
        private readonly StorageManager $storageManager = new StorageManager(),
        private readonly VersionService $versionService = new VersionService()
    ) {
    }

    public function upload(array $file, array $input, array $user): array
    {
        $connection = Database::connection();
        $uploadResult = null;
        $prepared = null;
        $now = new DateTimeImmutable();
        $createdAt = $now->format('Y-m-d H:i:s');

        try {
            $prepared = $this->prepareUpload($file, $input);

            $uploadResult = $this->storageManager->provider()->upload($prepared['tmp_path'], [
                'resource_type' => $prepared['media_type'] === 'video' ? 'video' : 'image',
                'folder' => $this->buildCloudinaryFolder((int) $user['business_id']),
                'public_id' => $this->generatePublicId((int) $user['business_id'], $prepared['media_type']),
                'use_filename' => false,
                'unique_filename' => false,
                'overwrite' => false,
                'invalidate' => true,
            ]);

            $connection->beginTransaction();

            $mediaId = $this->mediaRepository->create([
                'business_id' => (int) $user['business_id'],
                'title' => $prepared['title'],
                'original_filename' => $prepared['original_filename'],
                'description' => null,
                'media_type' => $prepared['media_type'],
                'mime_type' => $prepared['mime_type'],
                'file_size_bytes' => $prepared['file_size'],
                'duration_seconds' => $prepared['media_type'] === 'video'
                    ? ($uploadResult['duration_seconds'] ?? null)
                    : null,
                'width' => $prepared['width'] ?? $uploadResult['width'] ?? null,
                'height' => $prepared['height'] ?? $uploadResult['height'] ?? null,
                'checksum_sha256' => $prepared['checksum_sha256'],
                'storage_provider' => config('storage.default', 'cloudinary'),
                'storage_key' => $uploadResult['storage_key'],
                'storage_url' => $uploadResult['storage_url'],
                'thumbnail_url' => $uploadResult['thumbnail_url'],
                'status' => 'ready',
                'created_by' => (int) $user['id'],
                'created_at' => $createdAt,
                'updated_at' => $createdAt,
            ]);

            $this->syncChangeRepository->createChange(
                (int) $user['business_id'],
                'media',
                $mediaId,
                'created',
                [
                    'media_id' => $mediaId,
                    'title' => $prepared['title'],
                    'media_type' => $prepared['media_type'],
                    'checksum_sha256' => $prepared['checksum_sha256'],
                    'storage_key' => $uploadResult['storage_key'],
                    'status' => 'ready',
                    'updated_at' => $createdAt,
                ],
                $createdAt
            );
            $this->versionService->increment();

            $connection->commit();

            Logger::info('Media uploaded successfully.', [
                'media_id' => $mediaId,
                'business_id' => (int) $user['business_id'],
                'media_type' => $prepared['media_type'],
                'storage_key' => $uploadResult['storage_key'],
            ]);

            return [
                'success' => true,
                'media_id' => $mediaId,
                'url' => $uploadResult['storage_url'],
                'thumbnail' => $uploadResult['thumbnail_url'],
                'checksum' => $prepared['checksum_sha256'],
                'message' => 'Upload successful',
            ];
        } catch (Throwable $throwable) {
            if ($connection->inTransaction()) {
                $connection->rollBack();
            }

            if (is_array($uploadResult) && !empty($uploadResult['storage_key']) && is_array($prepared)) {
                try {
                    $this->storageManager->provider()->delete($uploadResult['storage_key'], [
                        'resource_type' => $prepared['media_type'] === 'video' ? 'video' : 'image',
                    ]);
                } catch (Throwable $cleanupThrowable) {
                    Logger::error('Failed to clean up uploaded media after a rollback.', [
                        'storage_key' => $uploadResult['storage_key'],
                        'error' => $cleanupThrowable->getMessage(),
                    ]);
                }
            }

            Logger::error('Media upload failed.', [
                'business_id' => (int) ($user['business_id'] ?? 0),
                'error' => $throwable->getMessage(),
            ]);

            throw $throwable;
        }
    }

    public function listForBusiness(int $businessId, int $limit = 48): array
    {
        return $this->mediaRepository->listRecentByBusiness($businessId, $limit);
    }

    private function prepareUpload(array $file, array $input): array
    {
        $this->assertValidUploadArray($file);

        if ((int) $file['error'] !== UPLOAD_ERR_OK) {
            throw new ValidationException($this->mapUploadError((int) $file['error']));
        }

        $tmpPath = (string) ($file['tmp_name'] ?? '');
        if (
            $tmpPath === '' ||
            (
                !is_uploaded_file($tmpPath) &&
                !(PHP_SAPI === 'cli' && is_file($tmpPath))
            )
        ) {
            throw new ValidationException('No valid uploaded file was received.');
        }

        $originalFilename = (string) ($file['name'] ?? 'upload');
        $extension = strtolower((string) pathinfo($originalFilename, PATHINFO_EXTENSION));
        $mediaType = $this->detectMediaType($extension);
        $mimeType = $this->detectMimeType($tmpPath);
        $this->validateMimeType($extension, $mimeType);

        $fileSize = (int) ($file['size'] ?? 0);
        $maxSizeBytes = (int) config('media.max_upload_size_mb', 100) * 1024 * 1024;

        if ($fileSize <= 0) {
            throw new ValidationException('Uploaded file is empty.');
        }

        if ($fileSize > $maxSizeBytes) {
            throw new ValidationException('Uploaded file exceeds the maximum allowed size of ' . config('media.max_upload_size_mb', 100) . ' MB.');
        }

        $checksum = hash_file('sha256', $tmpPath);
        if ($checksum === false) {
            throw new HttpException('Unable to generate file checksum.', 500);
        }

        $localMetadata = $this->extractLocalMetadata($tmpPath, $mediaType);

        return [
            'tmp_path' => $tmpPath,
            'title' => $this->normalizeTitle((string) ($input['title'] ?? ''), $originalFilename),
            'original_filename' => substr(basename($originalFilename), 0, 255),
            'mime_type' => $mimeType,
            'media_type' => $mediaType,
            'checksum_sha256' => $checksum,
            'file_size' => $fileSize,
            'width' => $localMetadata['width'] ?? null,
            'height' => $localMetadata['height'] ?? null,
        ];
    }

    private function assertValidUploadArray(array $file): void
    {
        if (!isset($file['error'], $file['name'], $file['tmp_name'], $file['size'])) {
            throw new ValidationException('The upload request is missing a valid file payload.');
        }
    }

    private function detectMediaType(string $extension): string
    {
        $images = config('media.allowed_extensions.image', []);
        $videos = config('media.allowed_extensions.video', []);

        if (in_array($extension, $images, true)) {
            return 'image';
        }

        if (in_array($extension, $videos, true)) {
            return 'video';
        }

        throw new ValidationException('Unsupported file extension [' . $extension . '].');
    }

    private function detectMimeType(string $tmpPath): string
    {
        $finfo = new \finfo(FILEINFO_MIME_TYPE);
        $mimeType = $finfo->file($tmpPath);

        if (!is_string($mimeType) || $mimeType === '') {
            throw new ValidationException('Unable to determine the uploaded file MIME type.');
        }

        return strtolower($mimeType);
    }

    private function validateMimeType(string $extension, string $mimeType): void
    {
        $allowedMimeTypes = config('media.allowed_mime_types', []);
        $expectedMimeTypes = $allowedMimeTypes[$extension] ?? [];

        if (!in_array($mimeType, $expectedMimeTypes, true)) {
            throw new ValidationException('The uploaded file MIME type is not allowed for the .' . $extension . ' extension.');
        }
    }

    private function extractLocalMetadata(string $tmpPath, string $mediaType): array
    {
        if ($mediaType !== 'image') {
            return [];
        }

        $imageInfo = @getimagesize($tmpPath);

        if (!is_array($imageInfo)) {
            throw new ValidationException('Unable to read image dimensions from the uploaded file.');
        }

        return [
            'width' => isset($imageInfo[0]) ? (int) $imageInfo[0] : null,
            'height' => isset($imageInfo[1]) ? (int) $imageInfo[1] : null,
        ];
    }

    private function normalizeTitle(string $title, string $originalFilename): string
    {
        $title = trim($title);

        if ($title === '') {
            $title = (string) pathinfo($originalFilename, PATHINFO_FILENAME);
        }

        $title = substr($title, 0, 180);

        if ($title === '') {
            throw new ValidationException('A media title could not be derived from the upload.');
        }

        return $title;
    }

    private function generatePublicId(int $businessId, string $mediaType): string
    {
        return sprintf(
            'business_%d_%s_%s',
            $businessId,
            $mediaType,
            bin2hex(random_bytes(12))
        );
    }

    private function buildCloudinaryFolder(int $businessId): string
    {
        return trim((string) config('storage.cloudinary.folder', 'playonmytv/media'), '/') . '/business_' . $businessId;
    }

    private function mapUploadError(int $errorCode): string
    {
        return match ($errorCode) {
            UPLOAD_ERR_INI_SIZE, UPLOAD_ERR_FORM_SIZE => 'Uploaded file exceeds the maximum allowed size.',
            UPLOAD_ERR_PARTIAL => 'Uploaded file was only partially received.',
            UPLOAD_ERR_NO_FILE => 'No file was uploaded.',
            UPLOAD_ERR_NO_TMP_DIR => 'Temporary upload directory is missing.',
            UPLOAD_ERR_CANT_WRITE => 'Failed to write uploaded file to disk.',
            UPLOAD_ERR_EXTENSION => 'A PHP extension stopped the file upload.',
            default => 'Upload failed due to an unknown error.',
        };
    }
}
