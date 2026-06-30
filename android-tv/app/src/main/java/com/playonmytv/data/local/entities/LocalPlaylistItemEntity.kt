package com.playonmytv.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_playlist_items",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["mediaId"]),
        Index(value = ["playlistId", "sortOrder"]),
    ],
)
data class LocalPlaylistItemEntity(
    @PrimaryKey val id: Long,
    val playlistId: Long,
    val mediaId: Long,
    val sortOrder: Int,
    val imageDurationSeconds: Int?,
)
