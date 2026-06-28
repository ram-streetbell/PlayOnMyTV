package com.playonmytv.domain.repository

import com.playonmytv.domain.model.DownloadProgress
import com.playonmytv.domain.model.DownloadResult
import com.playonmytv.domain.model.MediaDownloadRequest
import com.playonmytv.domain.model.MediaEntityModel

interface MediaRepository {
    suspend fun enqueueDownload(request: MediaDownloadRequest)

    suspend fun processDownload(
        request: MediaDownloadRequest,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ): DownloadResult

    suspend fun pauseDownload(mediaId: Long)

    suspend fun resumeDownload(request: MediaDownloadRequest)

    suspend fun cancelDownload(mediaId: Long)

    suspend fun findMedia(mediaId: Long): MediaEntityModel?

    suspend fun registerProgressCallback(mediaId: Long, callback: suspend (DownloadProgress) -> Unit)

    suspend fun clearProgressCallback(mediaId: Long)
}
