package com.playonmytv.player.download

import android.content.Context
import java.io.File
import java.security.MessageDigest

class MediaStorageHelper(private val context: Context) {
    fun finalFileFor(filename: String, mediaType: String): File {
        val directory = mediaDirectory(mediaType)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        return File(directory, sanitizeFilename(filename))
    }

    fun partialFileFor(filename: String, mediaType: String): File {
        return File(finalFileFor(filename, mediaType).absolutePath + ".part")
    }

    fun ensureFreshDownloadTarget(filename: String, mediaType: String) {
        finalFileFor(filename, mediaType).takeIf(File::exists)?.delete()
        partialFileFor(filename, mediaType).takeIf(File::exists)?.delete()
    }

    fun existingFileMatchesChecksum(filename: String, mediaType: String, checksum: String): Boolean {
        val finalFile = finalFileFor(filename, mediaType)
        return finalFile.exists() && sha256(finalFile).equals(checksum, ignoreCase = true)
    }

    fun commitPartialDownload(filename: String, mediaType: String): File {
        val finalFile = finalFileFor(filename, mediaType)
        val partialFile = partialFileFor(filename, mediaType)

        if (finalFile.exists()) {
            finalFile.delete()
        }

        if (!partialFile.renameTo(finalFile)) {
            throw IllegalStateException("Unable to move partial file into final storage.")
        }

        return finalFile
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }

        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun mediaDirectory(mediaType: String): File {
        val root = File(context.filesDir, "media")
        val child = if (mediaType.equals("video", ignoreCase = true)) "videos" else "images"
        return File(root, child)
    }

    private fun sanitizeFilename(filename: String): String {
        return filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}

