package com.playonmytv.domain.model

data class ManifestScheduleSlot(
    val id: Long,
    val playlistId: Long,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val priority: Int,
    val updatedAt: String?,
)

