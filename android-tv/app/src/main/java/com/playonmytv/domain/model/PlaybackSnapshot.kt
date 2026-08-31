package com.playonmytv.domain.model

data class PlaybackSnapshot(
    val candidate: LocalPlaybackCandidate?,
    val mediaItems: List<LocalPlaybackMediaItem>,
) {
    /**
     * A null candidate means there is no active schedule. If media has already
     * been synchronized locally, it is still valid playback content and should
     * not be treated as idle.
     */
    val isIdle: Boolean
        get() = mediaItems.isEmpty()
}
