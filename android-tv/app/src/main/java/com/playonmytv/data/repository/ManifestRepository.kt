package com.playonmytv.data.repository

import com.playonmytv.data.local.dao.MediaDao
import com.playonmytv.data.local.dao.SyncStateDao
import com.playonmytv.data.local.entities.MediaEntity
import com.playonmytv.data.local.entities.SyncStateEntity
import com.playonmytv.data.remote.ManifestApi
import com.playonmytv.domain.model.ManifestPayload

class ManifestRepository(
    private val manifestApi: ManifestApi,
    private val mediaDao: MediaDao,
    private val syncStateDao: SyncStateDao,
) {
    suspend fun fetchManifest(deviceToken: String): ManifestPayload {
        return manifestApi.fetchManifest(deviceToken)
    }

    suspend fun getLocalMedia(): List<MediaEntity> {
        return mediaDao.findAll()
    }

    suspend fun deleteLocalMedia(mediaId: Long) {
        mediaDao.deleteById(mediaId)
    }

    suspend fun getSyncState(): SyncStateEntity? {
        return syncStateDao.get()
    }

    suspend fun saveSyncState(syncState: SyncStateEntity) {
        syncStateDao.upsert(syncState)
    }
}
