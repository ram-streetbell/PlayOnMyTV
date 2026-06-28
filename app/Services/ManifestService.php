<?php

declare(strict_types=1);

namespace App\Services;

use App\Exceptions\HttpException;
use App\Repositories\ManifestRepository;
use DateTimeImmutable;

class ManifestService
{
    public function __construct(
        private readonly ManifestRepository $manifestRepository = new ManifestRepository(),
        private readonly VersionService $versionService = new VersionService()
    ) {
    }

    public function buildForDevice(array $device): array
    {
        $graph = $this->manifestRepository->fetchAssignedDeviceGraph(
            (int) $device['id'],
            (int) $device['business_id']
        );

        if (!is_array($graph['device'])) {
            throw new HttpException('Device manifest could not be generated.', 404);
        }

        $version = $this->versionService->current();

        return [
            'manifest_version' => (int) $version['current_version'],
            'generated_at' => (new DateTimeImmutable())->format(DATE_ATOM),
            'device' => $this->formatDevice($graph['device']),
            'assigned_playlists' => $this->formatPlaylists($graph['playlists']),
            'schedules' => $this->formatSchedules($graph['assignments'], $graph['schedules']),
            'media' => $this->formatMedia($graph['media']),
        ];
    }

    private function formatDevice(array $device): array
    {
        return [
            'id' => (int) $device['id'],
            'uuid' => $device['device_uuid'],
            'name' => $device['device_name'],
            'platform' => $device['platform'],
            'app_version' => $device['app_version'],
            'firmware_version' => $device['firmware_version'],
            'status' => $device['status'],
            'timezone' => $device['timezone'],
            'screen_resolution' => $device['screen_resolution'],
            'last_seen_at' => $device['last_seen_at'],
            'last_sync_at' => $device['last_sync_at'],
            'updated_at' => $device['updated_at'],
        ];
    }

    private function formatSchedules(array $assignments, array $scheduleRows): array
    {
        $schedules = [];

        foreach ($scheduleRows as $row) {
            $scheduleId = (int) $row['id'];

            if (!isset($schedules[$scheduleId])) {
                $assignment = $this->findAssignment($assignments, $scheduleId);
                $schedules[$scheduleId] = [
                    'id' => $scheduleId,
                    'name' => $row['name'],
                    'description' => $row['description'],
                    'status' => $row['status'],
                    'timezone' => $row['timezone'],
                    'assigned_at' => $assignment['assigned_at'] ?? null,
                    'updated_at' => $row['updated_at'],
                    'slots' => [],
                ];
            }

            if (!empty($row['slot_id'])) {
                $schedules[$scheduleId]['slots'][] = [
                    'id' => (int) $row['slot_id'],
                    'playlist_id' => (int) $row['playlist_id'],
                    'day_of_week' => (int) $row['day_of_week'],
                    'start_time' => $row['start_time'],
                    'end_time' => $row['end_time'],
                    'priority' => (int) $row['priority'],
                    'updated_at' => $row['slot_updated_at'],
                ];
            }
        }

        return array_values($schedules);
    }

    private function formatPlaylists(array $playlistRows): array
    {
        $playlists = [];

        foreach ($playlistRows as $row) {
            $playlistId = (int) $row['id'];

            if (!isset($playlists[$playlistId])) {
                $playlists[$playlistId] = [
                    'id' => $playlistId,
                    'name' => $row['name'],
                    'description' => $row['description'],
                    'status' => $row['status'],
                    'is_looping' => (bool) $row['is_looping'],
                    'updated_at' => $row['updated_at'],
                    'items' => [],
                ];
            }

            if (!empty($row['playlist_item_id'])) {
                $playlists[$playlistId]['items'][] = [
                    'id' => (int) $row['playlist_item_id'],
                    'media_id' => (int) $row['media_id'],
                    'sort_order' => (int) $row['sort_order'],
                    'image_duration_seconds' => $row['image_duration_seconds'] !== null
                        ? (int) $row['image_duration_seconds']
                        : null,
                    'updated_at' => $row['playlist_item_updated_at'],
                ];
            }
        }

        return array_values($playlists);
    }

    private function formatMedia(array $mediaRows): array
    {
        return array_map(static function (array $row): array {
            return [
                'id' => (int) $row['id'],
                'filename' => $row['original_filename'],
                'title' => $row['title'],
                'checksum' => $row['checksum_sha256'],
                'type' => $row['media_type'],
                'duration' => $row['duration_seconds'] !== null ? (int) $row['duration_seconds'] : null,
                'size' => (int) $row['file_size_bytes'],
                'width' => $row['width'] !== null ? (int) $row['width'] : null,
                'height' => $row['height'] !== null ? (int) $row['height'] : null,
                'storage_url' => $row['storage_url'],
                'thumbnail_url' => $row['thumbnail_url'],
                'updated_at' => $row['updated_at'],
            ];
        }, $mediaRows);
    }

    private function findAssignment(array $assignments, int $scheduleId): ?array
    {
        foreach ($assignments as $assignment) {
            if ((int) $assignment['schedule_id'] === $scheduleId) {
                return $assignment;
            }
        }

        return null;
    }
}

