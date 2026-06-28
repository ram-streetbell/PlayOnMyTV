package com.playonmytv.domain.model

data class MediaDownloadRequest(
    val id: Long,
    val mediaUrl: String,
    val checksum: String,
    val filename: String,
    val mediaType: String,
    val duration: Int?,
    val width: Int?,
    val height: Int?,
    val size: Long,
    val updatedAt: Long?,
)
