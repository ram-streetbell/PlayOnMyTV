package com.playonmytv.player.download

import com.playonmytv.domain.model.DownloadProgress
import com.playonmytv.domain.model.DownloadResult
import com.playonmytv.domain.model.MediaDownloadRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class MediaDownloader(
    private val okHttpClient: OkHttpClient,
    private val storageHelper: MediaStorageHelper,
    private val downloadDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(3),
) {
    private val activeCalls = ConcurrentHashMap<Long, Call>()

    suspend fun download(
        request: MediaDownloadRequest,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ): DownloadResult = withContext(downloadDispatcher) {
        val finalFile = storageHelper.finalFileFor(request.filename, request.mediaType)

        if (isUsableChecksum(request.checksum) &&
            storageHelper.existingFileMatchesChecksum(request.filename, request.mediaType, request.checksum)
        ) {
            return@withContext DownloadResult(
                mediaId = request.id,
                success = true,
                localPath = finalFile.absolutePath,
                checksum = request.checksum,
                skipped = true,
                message = "Existing file already matches checksum.",
            )
        }

        val partialFile = storageHelper.partialFileFor(request.filename, request.mediaType)

        if (finalFile.exists() &&
            (!isUsableChecksum(request.checksum) ||
                !storageHelper.existingFileMatchesChecksum(request.filename, request.mediaType, request.checksum))
        ) {
            storageHelper.ensureFreshDownloadTarget(request.filename, request.mediaType)
        }

        val downloadedBytes = if (partialFile.exists()) partialFile.length() else 0L

        val httpRequest = Request.Builder()
            .url(request.mediaUrl)
            .apply {
                if (downloadedBytes > 0) {
                    addHeader("Range", "bytes=$downloadedBytes-")
                }
            }
            .build()

        val call = okHttpClient.newCall(httpRequest)
        activeCalls[request.id] = call

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Download failed with HTTP ${response.code}.")
                }

                val responseBody = response.body
                    ?: throw IllegalStateException("Download response body was empty.")

                val restartFromBeginning = downloadedBytes > 0 && response.code == 200
                val append = downloadedBytes > 0 && response.code == 206
                if (restartFromBeginning) {
                    partialFile.delete()
                }

                val initialBytes = if (append) downloadedBytes else 0L
                val totalBytes = resolveTotalBytes(
                    response.header("Content-Range"),
                    responseBody.contentLength(),
                    initialBytes,
                )

                RandomAccessFile(partialFile, "rw").use { output ->
                    if (append) {
                        output.seek(downloadedBytes)
                    } else {
                        output.setLength(0)
                    }

                    responseBody.byteStream().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesCopied = initialBytes

                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break

                            output.write(buffer, 0, read)
                            bytesCopied += read

                            onProgress(
                                DownloadProgress(
                                    mediaId = request.id,
                                    bytesDownloaded = bytesCopied,
                                    totalBytes = totalBytes,
                                    percent = if (totalBytes > 0) {
                                        ((bytesCopied * 100) / totalBytes).toInt().coerceIn(0, 100)
                                    } else {
                                        0
                                    },
                                )
                            )
                        }
                    }
                }
            }

            val committedFile = storageHelper.commitPartialDownload(request.filename, request.mediaType)

            // The current web backend uses a version identifier in this field rather
            // than a SHA-256 digest. Only enforce checksum verification when the value
            // is actually a SHA-256 digest (64 hexadecimal characters).
            if (isUsableChecksum(request.checksum)) {
                val actualChecksum = storageHelper.sha256(committedFile)
                if (!actualChecksum.equals(request.checksum, ignoreCase = true)) {
                    committedFile.delete()
                    throw IllegalStateException("Checksum verification failed for media ${request.id}.")
                }
            }

            DownloadResult(
                mediaId = request.id,
                success = true,
                localPath = committedFile.absolutePath,
                checksum = request.checksum,
                skipped = false,
                message = "Download completed successfully.",
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            activeCalls.remove(request.id)
        }
    }

    fun cancel(mediaId: Long) {
        activeCalls.remove(mediaId)?.cancel()
    }

    private fun isUsableChecksum(value: String): Boolean =
        value.length == 64 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    private fun resolveTotalBytes(contentRange: String?, responseLength: Long, downloadedBytes: Long): Long {
        val totalFromHeader = contentRange
            ?.substringAfterLast('/')
            ?.toLongOrNull()

        return when {
            totalFromHeader != null -> totalFromHeader
            responseLength > 0 && downloadedBytes > 0 -> responseLength + downloadedBytes
            else -> responseLength
        }
    }
}
