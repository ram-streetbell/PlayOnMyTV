package com.playonmytv.domain.model

data class LocalPlaybackSchedule(
    val scheduleId: Long,
    val scheduleName: String,
    val timezone: String?,
    val status: String,
    val updatedAt: Long?,
)
