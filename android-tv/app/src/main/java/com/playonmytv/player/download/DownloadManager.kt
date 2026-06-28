package com.playonmytv.player.download

import com.playonmytv.domain.model.DownloadProgress
import com.playonmytv.domain.model.DownloadResult
import com.playonmytv.domain.model.MediaDownloadRequest

class DownloadManager(
    private val mediaDownloader: MediaDownloader,
) {
    suspend fun download(
        request: MediaDownloadRequest,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ): DownloadResult {
        return mediaDownloader.download(request, onProgress)
    }

    fun cancel(mediaId: Long) {
        mediaDownloader.cancel(mediaId)
    }
}
