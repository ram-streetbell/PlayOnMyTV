package com.playonmytv.domain.model

data class ManifestPlaylist(
    val id: Long,
    val name: String,
    val description: String?,
    val status: String,
    val isLooping: Boolean,
    val updatedAt: String?,
    val items: List<ManifestPlaylistItem>,
)

