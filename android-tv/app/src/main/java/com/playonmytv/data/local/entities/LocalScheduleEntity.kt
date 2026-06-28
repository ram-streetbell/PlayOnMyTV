package com.playonmytv.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_schedules")
data class LocalScheduleEntity(
    @PrimaryKey val id: Long
)

