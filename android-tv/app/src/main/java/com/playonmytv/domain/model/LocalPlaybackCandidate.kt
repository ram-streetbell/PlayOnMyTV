package com.playonmytv.domain.model

data class LocalPlaybackCandidate(
    val schedule: LocalPlaybackSchedule,
    val slot: LocalPlaybackSlot,
    val playlist: LocalPlaybackPlaylist,
)
