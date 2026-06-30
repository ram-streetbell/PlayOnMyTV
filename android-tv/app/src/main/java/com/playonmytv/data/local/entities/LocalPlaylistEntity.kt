package com.playonmytv.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_playlists",
    indices = [
        Index(value = ["status"]),
    ],
)
data class LocalPlaylistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val isLooping: Boolean,
    val status: String,
    val updatedAt: Long?,
)
