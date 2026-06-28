package com.playonmytv.domain.model

data class ManifestPlaylistItem(
    val id: Long,
    val mediaId: Long,
    val sortOrder: Int,
    val imageDurationSeconds: Int?,
    val updatedAt: String?,
)

