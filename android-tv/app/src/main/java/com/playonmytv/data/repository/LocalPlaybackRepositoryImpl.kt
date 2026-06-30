package com.playonmytv.data.repository

import com.playonmytv.data.local.dao.LocalActiveScheduleSlotRow
import com.playonmytv.data.local.dao.LocalPlaylistDao
import com.playonmytv.data.local.dao.LocalScheduleDao
import com.playonmytv.domain.model.LocalPlaybackCandidate
import com.playonmytv.domain.model.LocalPlaybackMediaItem
import com.playonmytv.domain.model.LocalPlaybackPlaylist
import com.playonmytv.domain.model.LocalPlaybackSchedule
import com.playonmytv.domain.model.LocalPlaybackSlot
import com.playonmytv.domain.model.PlaybackSnapshot
import com.playonmytv.domain.repository.LocalPlaybackRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalPlaybackRepositoryImpl(
    private val localPlaylistDao: LocalPlaylistDao,
    private val localScheduleDao: LocalScheduleDao,
) : LocalPlaybackRepository {
    override suspend fun getCurrentSchedule(
        backendDayOfWeek: Int,
        currentTime: String,
    ): LocalPlaybackSchedule? {
        return localScheduleDao.findActiveScheduleSlot(
            backendDayOfWeek = backendDayOfWeek,
            currentTime = currentTime,
            activeStatus = ACTIVE_STATUS,
        )?.toSchedule()
    }

    override suspend fun getCurrentSlot(
        backendDayOfWeek: Int,
        currentTime: String,
    ): LocalPlaybackSlot? {
        return localScheduleDao.findActiveScheduleSlot(
            backendDayOfWeek = backendDayOfWeek,
            currentTime = currentTime,
            activeStatus = ACTIVE_STATUS,
        )?.toSlot()
    }

    override suspend fun getCurrentActivePlaylist(
        backendDayOfWeek: Int,
        currentTime: String,
    ): LocalPlaybackPlaylist? {
        return localScheduleDao.findActiveScheduleSlot(
            backendDayOfWeek = backendDayOfWeek,
            currentTime = currentTime,
            activeStatus = ACTIVE_STATUS,
        )?.toPlaylist()
    }

    override suspend fun getOrderedMediaList(playlistId: Long): List<LocalPlaybackMediaItem> {
        return localPlaylistDao.getOrderedMediaRows(
            playlistId = playlistId,
            completedStatus = COMPLETED_STATUS,
        ).map { row ->
            LocalPlaybackMediaItem(
                playlistItemId = row.playlistItemId,
                playlistId = row.playlistId,
                mediaId = row.mediaId,
                sortOrder = row.sortOrder,
                imageDurationSeconds = row.imageDurationSeconds,
                filename = row.filename,
                path = row.path,
                mediaType = row.mediaType,
                checksum = row.checksum,
                duration = row.duration,
                width = row.width,
                height = row.height,
                size = row.size,
                updatedAt = row.updatedAt,
            )
        }
    }

    override suspend fun getScheduleCandidates(): List<LocalPlaybackCandidate> {
        return localScheduleDao.findScheduleCandidates(activeStatus = ACTIVE_STATUS)
            .map { row ->
                LocalPlaybackCandidate(
                    schedule = row.toSchedule(),
                    slot = row.toSlot(),
                    playlist = row.toPlaylist(),
                )
            }
    }

    override suspend fun getPlaybackSnapshot(candidate: LocalPlaybackCandidate?): PlaybackSnapshot {
        if (candidate == null) {
            return PlaybackSnapshot(
                candidate = null,
                mediaItems = emptyList(),
            )
        }

        return PlaybackSnapshot(
            candidate = candidate,
            mediaItems = getOrderedMediaList(candidate.playlist.playlistId),
        )
    }

    override fun observeScheduleCandidates(pollIntervalMillis: Long): Flow<List<LocalPlaybackCandidate>> = flow {
        while (true) {
            emit(getScheduleCandidates())
            delay(pollIntervalMillis)
        }
    }

    private fun LocalActiveScheduleSlotRow.toSchedule(): LocalPlaybackSchedule {
        return LocalPlaybackSchedule(
            scheduleId = scheduleId,
            scheduleName = scheduleName,
            timezone = scheduleTimezone,
            status = scheduleStatus,
            updatedAt = scheduleUpdatedAt,
        )
    }

    private fun LocalActiveScheduleSlotRow.toSlot(): LocalPlaybackSlot {
        return LocalPlaybackSlot(
            slotId = slotId,
            scheduleId = scheduleId,
            playlistId = playlistId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            priority = priority,
            updatedAt = slotUpdatedAt,
        )
    }

    private fun LocalActiveScheduleSlotRow.toPlaylist(): LocalPlaybackPlaylist {
        return LocalPlaybackPlaylist(
            playlistId = playlistId,
            playlistName = playlistName,
            isLooping = playlistLooping,
            status = playlistStatus,
            updatedAt = playlistUpdatedAt,
        )
    }

    companion object {
        private const val ACTIVE_STATUS = "active"
        private const val COMPLETED_STATUS = "COMPLETED"
    }
}
