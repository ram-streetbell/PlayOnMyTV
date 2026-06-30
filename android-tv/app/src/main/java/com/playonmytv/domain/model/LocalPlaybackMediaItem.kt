package com.playonmytv.domain.model

data class LocalPlaybackMediaItem(
    val playlistItemId: Long,
    val playlistId: Long,
    val mediaId: Long,
    val sortOrder: Int,
    val imageDurationSeconds: Int?,
    val filename: String,
    val path: String,
    val mediaType: String,
    val checksum: String,
    val duration: Int?,
    val width: Int?,
    val height: Int?,
    val size: Long,
    val updatedAt: Long,
)
