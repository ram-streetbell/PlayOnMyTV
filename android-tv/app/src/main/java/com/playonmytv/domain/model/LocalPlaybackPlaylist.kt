package com.playonmytv.domain.model

data class LocalPlaybackPlaylist(
    val playlistId: Long,
    val playlistName: String,
    val isLooping: Boolean,
    val status: String,
    val updatedAt: Long?,
)
