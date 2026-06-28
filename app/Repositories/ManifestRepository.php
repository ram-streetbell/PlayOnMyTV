<?php

declare(strict_types=1);

namespace App\Repositories;

class ManifestRepository extends BaseRepository
{
    public function fetchAssignedDeviceGraph(int $deviceId, int $businessId): array
    {
        $deviceStatement = $this->connection->prepare(
            'SELECT id,
                    business_id,
                    device_uuid,
                    device_name,
                    platform,
                    app_version,
                    firmware_version,
                    status,
                    timezone,
                    screen_resolution,
                    last_seen_at,
                    last_sync_at,
                    updated_at
             FROM devices
             WHERE id = :device_id
               AND business_id = :business_id
               AND status = :status
             LIMIT 1'
        );
        $deviceStatement->execute([
            'device_id' => $deviceId,
            'business_id' => $businessId,
            'status' => 'active',
        ]);
        $device = $deviceStatement->fetch();

        if (!is_array($device)) {
            return [
                'device' => null,
                'assignments' => [],
                'schedules' => [],
                'playlists' => [],
                'media' => [],
            ];
        }

        $assignmentStatement = $this->connection->prepare(
            'SELECT id,
                    device_id,
                    schedule_id,
                    is_active,
                    assigned_at,
                    updated_at
             FROM device_schedule_assignments
             WHERE device_id = :device_id
               AND is_active = 1'
        );
        $assignmentStatement->execute([
            'device_id' => $deviceId,
        ]);
        $assignments = $assignmentStatement->fetchAll() ?: [];

        if ($assignments === []) {
            return [
                'device' => $device,
                'assignments' => [],
                'schedules' => [],
                'playlists' => [],
                'media' => [],
            ];
        }

        $scheduleIds = array_values(array_unique(array_map(
            static fn (array $assignment): int => (int) $assignment['schedule_id'],
            $assignments
        )));

        $schedulePlaceholders = $this->createPlaceholders($scheduleIds, 'schedule_id');
        $scheduleStatement = $this->connection->prepare(
            'SELECT s.id,
                    s.name,
                    s.description,
                    s.status,
                    s.timezone,
                    s.updated_at,
                    ss.id AS slot_id,
                    ss.playlist_id,
                    ss.day_of_week,
                    ss.start_time,
                    ss.end_time,
                    ss.priority,
                    ss.updated_at AS slot_updated_at
             FROM schedules s
             LEFT JOIN schedule_slots ss ON ss.schedule_id = s.id
             WHERE s.id IN (' . $schedulePlaceholders['sql'] . ')
               AND s.business_id = :business_id
             ORDER BY s.id, ss.day_of_week, ss.start_time, ss.id'
        );
        $scheduleStatement->execute($schedulePlaceholders['bindings'] + [
            'business_id' => $businessId,
        ]);
        $scheduleRows = $scheduleStatement->fetchAll() ?: [];

        $playlistIds = [];
        foreach ($scheduleRows as $row) {
            if (!empty($row['playlist_id'])) {
                $playlistIds[] = (int) $row['playlist_id'];
            }
        }
        $playlistIds = array_values(array_unique($playlistIds));

        $playlists = [];
        $mediaRows = [];

        if ($playlistIds !== []) {
            $playlistPlaceholders = $this->createPlaceholders($playlistIds, 'playlist_id');
            $playlistStatement = $this->connection->prepare(
                'SELECT p.id,
                        p.name,
                        p.description,
                        p.status,
                        p.is_looping,
                        p.updated_at,
                        pi.id AS playlist_item_id,
                        pi.media_id,
                        pi.sort_order,
                        pi.image_duration_seconds,
                        pi.updated_at AS playlist_item_updated_at
                 FROM playlists p
                 LEFT JOIN playlist_items pi ON pi.playlist_id = p.id
                 WHERE p.id IN (' . $playlistPlaceholders['sql'] . ')
                   AND p.business_id = :business_id
                 ORDER BY p.id, pi.sort_order, pi.id'
            );
            $playlistStatement->execute($playlistPlaceholders['bindings'] + [
                'business_id' => $businessId,
            ]);
            $playlists = $playlistStatement->fetchAll() ?: [];

            $mediaIds = [];
            foreach ($playlists as $playlist) {
                if (!empty($playlist['media_id'])) {
                    $mediaIds[] = (int) $playlist['media_id'];
                }
            }
            $mediaIds = array_values(array_unique($mediaIds));

            if ($mediaIds !== []) {
                $mediaPlaceholders = $this->createPlaceholders($mediaIds, 'media_id');
                $mediaStatement = $this->connection->prepare(
                    'SELECT id,
                            original_filename,
                            title,
                            checksum_sha256,
                            media_type,
                            duration_seconds,
                            file_size_bytes,
                            width,
                            height,
                            storage_url,
                            thumbnail_url,
                            updated_at
                     FROM media
                     WHERE id IN (' . $mediaPlaceholders['sql'] . ')
                       AND business_id = :business_id
                       AND deleted_at IS NULL
                       AND status = :status
                     ORDER BY id'
                );
                $mediaStatement->execute($mediaPlaceholders['bindings'] + [
                    'business_id' => $businessId,
                    'status' => 'ready',
                ]);
                $mediaRows = $mediaStatement->fetchAll() ?: [];
            }
        }

        return [
            'device' => $device,
            'assignments' => $assignments,
            'schedules' => $scheduleRows,
            'playlists' => $playlists,
            'media' => $mediaRows,
        ];
    }

    private function createPlaceholders(array $ids, string $prefix): array
    {
        $bindings = [];
        $parts = [];

        foreach (array_values($ids) as $index => $id) {
            $key = $prefix . '_' . $index;
            $parts[] = ':' . $key;
            $bindings[$key] = $id;
        }

        return [
            'sql' => implode(', ', $parts),
            'bindings' => $bindings,
        ];
    }
}

