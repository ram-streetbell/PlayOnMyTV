package com.playonmytv.data.local.dao

data class LocalActiveScheduleSlotRow(
    val slotId: Long,
    val scheduleId: Long,
    val playlistId: Long,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val priority: Int,
    val slotUpdatedAt: Long?,
    val scheduleName: String,
    val scheduleTimezone: String?,
    val scheduleStatus: String,
    val scheduleUpdatedAt: Long?,
    val playlistName: String,
    val playlistLooping: Boolean,
    val playlistStatus: String,
    val playlistUpdatedAt: Long?,
)
