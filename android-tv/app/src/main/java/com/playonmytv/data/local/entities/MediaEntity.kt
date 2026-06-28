package com.playonmytv.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val id: Long,
    val checksum: String,
    val filename: String,
    val path: String,
    val mediaType: String,
    val duration: Int?,
    val width: Int?,
    val height: Int?,
    val size: Long,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

