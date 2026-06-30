package com.playonmytv.data.repository

import androidx.room.withTransaction
import com.playonmytv.data.local.dao.LocalPlaylistDao
import com.playonmytv.data.local.dao.LocalScheduleDao
import com.playonmytv.data.local.db.AppDatabase
import com.playonmytv.data.local.entities.LocalPlaylistEntity
import com.playonmytv.data.local.entities.LocalPlaylistItemEntity
import com.playonmytv.data.local.entities.LocalScheduleEntity
import com.playonmytv.data.local.entities.LocalScheduleSlotEntity
import com.playonmytv.data.local.dao.MediaDao
import com.playonmytv.data.local.dao.SyncStateDao
import com.playonmytv.data.local.entities.MediaEntity
import com.playonmytv.data.local.entities.SyncStateEntity
import com.playonmytv.data.remote.ManifestApi
import com.playonmytv.domain.model.ManifestPayload
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ManifestRepository(
    private val database: AppDatabase,
    private val manifestApi: ManifestApi,
    private val mediaDao: MediaDao,
    private val syncStateDao: SyncStateDao,
    private val localPlaylistDao: LocalPlaylistDao,
    private val localScheduleDao: LocalScheduleDao,
) {
    suspend fun fetchManifest(deviceToken: String): ManifestPayload {
        return manifestApi.fetchManifest(deviceToken)
    }

    suspend fun getLocalMedia(): List<MediaEntity> {
        return mediaDao.findAll()
    }

    suspend fun deleteLocalMedia(mediaId: Long) {
        mediaDao.deleteById(mediaId)
    }

    suspend fun getSyncState(): SyncStateEntity? {
        return syncStateDao.get()
    }

    suspend fun saveSyncState(syncState: SyncStateEntity) {
        syncStateDao.upsert(syncState)
    }

    suspend fun replaceManifestStructure(manifest: ManifestPayload) {
        val playlists = manifest.assignedPlaylists.map { playlist ->
            LocalPlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                isLooping = playlist.isLooping,
                status = playlist.status,
                updatedAt = parseTimestamp(playlist.updatedAt),
            )
        }
        val playlistItems = manifest.assignedPlaylists
            .flatMap { playlist ->
                playlist.items.map { item ->
                    LocalPlaylistItemEntity(
                        id = item.id,
                        playlistId = playlist.id,
                        mediaId = item.mediaId,
                        sortOrder = item.sortOrder,
                        imageDurationSeconds = item.imageDurationSeconds,
                    )
                }
            }
        val schedules = manifest.schedules.map { schedule ->
            LocalScheduleEntity(
                id = schedule.id,
                name = schedule.name,
                timezone = schedule.timezone,
                status = schedule.status,
                updatedAt = parseTimestamp(schedule.updatedAt),
            )
        }
        val scheduleSlots = manifest.schedules
            .flatMap { schedule ->
                schedule.slots.map { slot ->
                    LocalScheduleSlotEntity(
                        id = slot.id,
                        scheduleId = schedule.id,
                        playlistId = slot.playlistId,
                        dayOfWeek = slot.dayOfWeek,
                        startTime = slot.startTime,
                        endTime = slot.endTime,
                        priority = slot.priority,
                        updatedAt = parseTimestamp(slot.updatedAt),
                    )
                }
            }

        database.withTransaction {
            localScheduleDao.clearScheduleSlots()
            localScheduleDao.clearSchedules()
            localPlaylistDao.clearPlaylistItems()
            localPlaylistDao.clearPlaylists()

            if (playlists.isNotEmpty()) {
                localPlaylistDao.upsertPlaylists(playlists)
            }
            if (playlistItems.isNotEmpty()) {
                localPlaylistDao.upsertPlaylistItems(playlistItems)
            }
            if (schedules.isNotEmpty()) {
                localScheduleDao.upsertSchedules(schedules)
            }
            if (scheduleSlots.isNotEmpty()) {
                localScheduleDao.upsertScheduleSlots(scheduleSlots)
            }
        }
    }

    suspend fun hasLocalPlaybackSnapshot(): Boolean {
        return localPlaylistDao.countPlaylists() > 0 || localScheduleDao.countSchedules() > 0
    }

    private fun parseTimestamp(value: String?): Long? {
        if (value.isNullOrBlank()) {
            return null
        }

        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value, MYSQL_TIMESTAMP_FORMAT)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            }.getOrNull()
    }

    companion object {
        private val MYSQL_TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
