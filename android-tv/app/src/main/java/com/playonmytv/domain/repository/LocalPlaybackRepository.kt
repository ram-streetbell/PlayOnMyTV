package com.playonmytv.domain.repository

import com.playonmytv.domain.model.LocalPlaybackCandidate
import com.playonmytv.domain.model.LocalPlaybackMediaItem
import com.playonmytv.domain.model.LocalPlaybackPlaylist
import com.playonmytv.domain.model.LocalPlaybackSchedule
import com.playonmytv.domain.model.LocalPlaybackSlot
import com.playonmytv.domain.model.PlaybackSnapshot
import kotlinx.coroutines.flow.Flow

interface LocalPlaybackRepository {
    suspend fun getCurrentSchedule(backendDayOfWeek: Int, currentTime: String): LocalPlaybackSchedule?

    suspend fun getCurrentSlot(backendDayOfWeek: Int, currentTime: String): LocalPlaybackSlot?

    suspend fun getCurrentActivePlaylist(backendDayOfWeek: Int, currentTime: String): LocalPlaybackPlaylist?

    suspend fun getOrderedMediaList(playlistId: Long): List<LocalPlaybackMediaItem>

    suspend fun getScheduleCandidates(): List<LocalPlaybackCandidate>

    suspend fun getPlaybackSnapshot(candidate: LocalPlaybackCandidate?): PlaybackSnapshot

    fun observeScheduleCandidates(pollIntervalMillis: Long = 5_000L): Flow<List<LocalPlaybackCandidate>>
}
