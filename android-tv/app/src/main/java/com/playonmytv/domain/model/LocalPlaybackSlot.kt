package com.playonmytv.domain.model

data class LocalPlaybackSlot(
    val slotId: Long,
    val scheduleId: Long,
    val playlistId: Long,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val priority: Int,
    val updatedAt: Long?,
)
