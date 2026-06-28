package com.playonmytv.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_media")
data class LocalMediaEntity(
    @PrimaryKey val id: Long
)

