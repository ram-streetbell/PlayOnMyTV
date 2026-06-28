package com.playonmytv.domain.model

data class ManifestSchedule(
    val id: Long,
    val name: String,
    val description: String?,
    val status: String,
    val timezone: String?,
    val assignedAt: String?,
    val updatedAt: String?,
    val slots: List<ManifestScheduleSlot>,
)

