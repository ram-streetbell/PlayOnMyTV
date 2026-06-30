package com.playonmytv.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_schedule_slots",
    indices = [
        Index(value = ["scheduleId"]),
        Index(value = ["playlistId"]),
        Index(value = ["dayOfWeek", "startTime", "endTime", "priority"]),
    ],
)
data class LocalScheduleSlotEntity(
    @PrimaryKey val id: Long,
    val scheduleId: Long,
    val playlistId: Long,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val priority: Int,
    val updatedAt: Long?,
)
