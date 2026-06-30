package com.playonmytv.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_schedules",
    indices = [
        Index(value = ["status"]),
    ],
)
data class LocalScheduleEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val timezone: String?,
    val status: String,
    val updatedAt: Long?,
)
