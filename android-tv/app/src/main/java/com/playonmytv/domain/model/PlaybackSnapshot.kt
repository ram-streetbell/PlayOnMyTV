package com.playonmytv.domain.model

data class PlaybackSnapshot(
    val candidate: LocalPlaybackCandidate?,
    val mediaItems: List<LocalPlaybackMediaItem>,
) {
    val isIdle: Boolean
        get() = candidate == null || mediaItems.isEmpty()
}
