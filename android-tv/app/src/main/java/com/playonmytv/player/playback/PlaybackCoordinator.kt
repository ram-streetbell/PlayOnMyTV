package com.playonmytv.player.playback

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.playonmytv.domain.model.LocalPlaybackCandidate
import com.playonmytv.domain.model.LocalPlaybackMediaItem
import com.playonmytv.domain.model.PlaybackSnapshot
import com.playonmytv.domain.repository.LocalPlaybackRepository
import com.playonmytv.player.scheduler.ScheduleEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class PlaybackCoordinator(
    private val context: Context,
    private val repository: LocalPlaybackRepository,
    private val scheduleEvaluator: ScheduleEvaluator,
    private val playerView: PlayerView,
    private val imageView: ImageView,
    private val idleTextView: TextView,
) {
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private var scope: CoroutineScope? = null
    private var observerJob: Job? = null
    private var imageTimerJob: Job? = null
    private var currentSnapshot: PlaybackSnapshot = PlaybackSnapshot(null, emptyList())
    private var pendingSnapshot: PlaybackSnapshot? = null
    private var currentIndex: Int = 0
    private var currentMediaItem: LocalPlaybackMediaItem? = null
    private var isStarted = false

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val item = currentMediaItem
            Log.e(
                TAG,
                "event=media_skipped reason=player_error mediaId=${item?.mediaId ?: -1} message=${error.message}",
                error,
            )
            onCurrentItemFinished(reason = "player_error", logCompleted = false)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && exoPlayer.playWhenReady) {
                Log.i(TAG, "event=playback_resumed")
            } else if (playbackState == Player.STATE_ENDED) {
                Log.i(TAG, "event=video_completed mediaId=${currentMediaItem?.mediaId ?: -1}")
                onCurrentItemFinished(reason = "video_completed")
            }
        }
    }

    fun start(scope: CoroutineScope) {
        if (isStarted) {
            return
        }

        this.scope = scope
        isStarted = true
        playerView.player = exoPlayer
        exoPlayer.addListener(playerListener)
        Log.i(TAG, "event=player_initialized")

        observerJob = scope.launch {
            repository.observeScheduleCandidates().collectLatest { candidates ->
                val selectedCandidate = scheduleEvaluator.selectActiveCandidate(candidates)
                val snapshot = repository.getPlaybackSnapshot(selectedCandidate)
                handleSnapshot(snapshot)
            }
        }
    }

    fun stop() {
        observerJob?.cancel()
        observerJob = null
        stopCurrentPlayback(showIdle = true)
        Log.i(TAG, "event=playback_stopped")
    }

    fun release() {
        stop()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    fun playNext() {
        onCurrentItemFinished(reason = "manual_next")
    }

    fun playPrevious() {
        val snapshot = activeSnapshotOrPending() ?: return
        if (snapshot.mediaItems.isEmpty()) {
            return
        }

        val lastIndex = snapshot.mediaItems.lastIndex
        currentIndex = when {
            currentIndex > 0 -> currentIndex - 1
            snapshot.candidate?.playlist?.isLooping == true -> lastIndex
            else -> 0
        }
        playCurrent(snapshot, fromScheduleChange = false)
    }

    private suspend fun handleSnapshot(snapshot: PlaybackSnapshot) {
        val previousCandidate = currentSnapshot.candidate
        val currentCandidate = snapshot.candidate
        val changed = snapshot != currentSnapshot

        if (!changed) {
            return
        }

        if (previousCandidate?.slot?.slotId != currentCandidate?.slot?.slotId) {
            Log.i(
                TAG,
                "event=schedule_changed previousSlot=${previousCandidate?.slot?.slotId ?: -1} currentSlot=${currentCandidate?.slot?.slotId ?: -1}"
            )
        }

        if (!isMediaInProgress()) {
            applySnapshot(snapshot, fromScheduleChange = true)
            return
        }

        pendingSnapshot = snapshot
    }

    private fun applySnapshot(
        snapshot: PlaybackSnapshot,
        fromScheduleChange: Boolean,
    ) {
        currentSnapshot = snapshot
        pendingSnapshot = null

        if (snapshot.isIdle) {
            showIdle("No scheduled content")
            return
        }

        currentIndex = resolveStartingIndex(snapshot)
        val candidate = snapshot.candidate ?: return
        Log.i(
            TAG,
            "event=schedule_selected scheduleId=${candidate.schedule.scheduleId} slotId=${candidate.slot.slotId} playlistId=${candidate.playlist.playlistId}"
        )
        Log.i(
            TAG,
            "event=playlist_loaded playlistId=${candidate.playlist.playlistId} items=${snapshot.mediaItems.size} looping=${candidate.playlist.isLooping}"
        )
        if (fromScheduleChange) {
            Log.i(TAG, "event=playlist_refreshed playlistId=${candidate.playlist.playlistId}")
        }
        playCurrent(snapshot, fromScheduleChange)
    }

    private fun playCurrent(
        snapshot: PlaybackSnapshot,
        fromScheduleChange: Boolean,
    ) {
        if (snapshot.mediaItems.isEmpty()) {
            showIdle("No scheduled content")
            return
        }

        val boundedIndex = currentIndex.coerceIn(0, snapshot.mediaItems.lastIndex)
        currentIndex = boundedIndex

        val candidate = snapshot.candidate ?: run {
            showIdle("No scheduled content")
            return
        }

        var attempts = 0
        var index = boundedIndex

        while (attempts < snapshot.mediaItems.size) {
            val item = snapshot.mediaItems[index]
            val file = File(item.path)

            if (!file.exists()) {
                Log.e(TAG, "event=media_skipped reason=missing_file mediaId=${item.mediaId} path=${item.path}")
                index = computeNextIndex(index, snapshot)
                attempts += 1
                continue
            }

            currentIndex = index
            currentMediaItem = item

            if (isImage(item)) {
                if (displayImage(item, file.toUri())) {
                    return
                }
            } else if (isVideo(item)) {
                playVideo(item, file.toUri(), candidate.playlist.isLooping && snapshot.mediaItems.size == 1)
                return
            } else {
                Log.e(TAG, "event=media_skipped reason=unsupported_type mediaId=${item.mediaId} type=${item.mediaType}")
            }

            index = computeNextIndex(index, snapshot)
            attempts += 1
        }

        showIdle("No scheduled content")
    }

    private fun displayImage(item: LocalPlaybackMediaItem, uri: Uri): Boolean {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                BitmapFactory.decodeFile(uri.path)
            } ?: return false

            stopVideoPlayback()
            imageView.visibility = View.VISIBLE
            playerView.visibility = View.GONE
            idleTextView.visibility = View.GONE
            imageView.setImageBitmap(bitmap)
            val durationSeconds = item.imageDurationSeconds ?: DEFAULT_IMAGE_DURATION_SECONDS
            Log.i(TAG, "event=media_started mediaId=${item.mediaId} type=image durationSeconds=$durationSeconds")

            imageTimerJob?.cancel()
            imageTimerJob = scope?.launch {
                delay(durationSeconds * 1_000L)
                Log.i(TAG, "event=image_timeout mediaId=${item.mediaId}")
                onCurrentItemFinished(reason = "image_timeout")
            }
            true
        } catch (exception: Exception) {
            Log.e(TAG, "event=media_skipped reason=image_decode_failed mediaId=${item.mediaId} message=${exception.message}", exception)
            false
        }
    }

    private fun playVideo(item: LocalPlaybackMediaItem, uri: Uri, loopSingleItem: Boolean) {
        imageTimerJob?.cancel()
        imageTimerJob = null
        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE
        idleTextView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        exoPlayer.repeatMode = if (loopSingleItem) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        Log.i(TAG, "event=media_started mediaId=${item.mediaId} type=video")
    }

    private fun onCurrentItemFinished(
        reason: String,
        logCompleted: Boolean = true,
    ) {
        if (logCompleted) {
            Log.i(TAG, "event=media_completed mediaId=${currentMediaItem?.mediaId ?: -1} reason=$reason")
        }

        val snapshotToApply = pendingSnapshot
        if (snapshotToApply != null) {
            currentMediaItem = null
            applySnapshot(snapshotToApply, fromScheduleChange = true)
            return
        }

        val snapshot = activeSnapshotOrPending() ?: return
        if (snapshot.mediaItems.isEmpty()) {
            showIdle("No scheduled content")
            return
        }

        val nextIndex = when {
            currentIndex < snapshot.mediaItems.lastIndex -> currentIndex + 1
            snapshot.candidate?.playlist?.isLooping == true -> {
                Log.i(TAG, "event=playlist_looped playlistId=${snapshot.candidate.playlist.playlistId}")
                0
            }
            else -> {
                showIdle("No scheduled content")
                return
            }
        }

        currentIndex = nextIndex
        currentMediaItem = null
        playCurrent(snapshot, fromScheduleChange = false)
    }

    private fun stopCurrentPlayback(showIdle: Boolean) {
        imageTimerJob?.cancel()
        imageTimerJob = null
        stopVideoPlayback()
        currentMediaItem = null
        if (showIdle) {
            showIdle("No scheduled content")
        }
    }

    private fun showIdle(message: String) {
        imageTimerJob?.cancel()
        imageTimerJob = null
        stopVideoPlayback()
        currentMediaItem = null
        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE
        playerView.visibility = View.GONE
        idleTextView.visibility = View.VISIBLE
        idleTextView.text = message
    }

    private fun stopVideoPlayback() {
        if (exoPlayer.isPlaying || exoPlayer.playbackState != Player.STATE_IDLE) {
            exoPlayer.stop()
        }
        exoPlayer.clearMediaItems()
    }

    private fun computeNextIndex(index: Int, snapshot: PlaybackSnapshot): Int {
        return when {
            index < snapshot.mediaItems.lastIndex -> index + 1
            snapshot.candidate?.playlist?.isLooping == true -> 0
            else -> snapshot.mediaItems.lastIndex
        }
    }

    private fun resolveStartingIndex(snapshot: PlaybackSnapshot): Int {
        val previousMediaId = currentMediaItem?.mediaId ?: return 0
        val matchedIndex = snapshot.mediaItems.indexOfFirst { it.mediaId == previousMediaId }
        if (matchedIndex == -1) {
            return 0
        }

        return if (matchedIndex < snapshot.mediaItems.lastIndex) matchedIndex + 1
        else if (snapshot.candidate?.playlist?.isLooping == true) 0
        else matchedIndex
    }

    private fun activeSnapshotOrPending(): PlaybackSnapshot? {
        return pendingSnapshot ?: currentSnapshot.takeIf { !it.isIdle || it.candidate != null }
    }

    private fun isMediaInProgress(): Boolean {
        return currentMediaItem != null && (imageTimerJob?.isActive == true || exoPlayer.isPlaying || exoPlayer.playbackState == Player.STATE_BUFFERING)
    }

    private fun isImage(item: LocalPlaybackMediaItem): Boolean {
        return item.mediaType.lowercase(Locale.US) == "image"
    }

    private fun isVideo(item: LocalPlaybackMediaItem): Boolean {
        return item.mediaType.lowercase(Locale.US) == "video"
    }

    companion object {
        private const val TAG = "PlaybackCoordinator"
        private const val DEFAULT_IMAGE_DURATION_SECONDS = 10
    }
}
