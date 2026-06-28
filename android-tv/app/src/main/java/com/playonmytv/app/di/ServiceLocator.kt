package com.playonmytv.app.di

import android.content.Context
import androidx.room.Room
import com.playonmytv.data.local.db.AppDatabase
import com.playonmytv.data.local.preferences.PreferenceStore
import com.playonmytv.data.remote.ApiService
import com.playonmytv.data.remote.ManifestApi
import com.playonmytv.data.repository.DownloadRepository
import com.playonmytv.data.repository.DeviceRepositoryImpl
import com.playonmytv.data.repository.ManifestRepository
import com.playonmytv.domain.repository.DeviceRepository
import com.playonmytv.domain.repository.MediaRepository
import com.playonmytv.player.download.DownloadManager
import com.playonmytv.player.download.MediaDownloader
import com.playonmytv.player.download.MediaStorageHelper
import okhttp3.OkHttpClient
import com.playonmytv.sync.ManifestComparator
import com.playonmytv.sync.ManifestSyncService

object ServiceLocator {
    @Volatile
    private var deviceRepository: DeviceRepository? = null
    @Volatile
    private var mediaRepository: MediaRepository? = null
    @Volatile
    private var appDatabase: AppDatabase? = null
    @Volatile
    private var okHttpClient: OkHttpClient? = null
    @Volatile
    private var downloadManager: DownloadManager? = null
    @Volatile
    private var manifestApi: ManifestApi? = null
    @Volatile
    private var manifestRepository: ManifestRepository? = null
    @Volatile
    private var manifestComparator: ManifestComparator? = null
    @Volatile
    private var manifestSyncService: ManifestSyncService? = null

    fun provideDeviceRepository(context: Context): DeviceRepository {
        return deviceRepository ?: synchronized(this) {
            deviceRepository ?: DeviceRepositoryImpl(
                apiService = ApiService(),
                preferenceStore = PreferenceStore(context.applicationContext)
            ).also { deviceRepository = it }
        }
    }

    fun provideMediaRepository(context: Context): MediaRepository {
        return mediaRepository ?: synchronized(this) {
            mediaRepository ?: DownloadRepository(
                appContext = context.applicationContext,
                mediaDao = provideDatabase(context).mediaDao(),
                downloadManager = provideDownloadManager(context)
            ).also { mediaRepository = it }
        }
    }

    fun provideManifestSyncService(context: Context): ManifestSyncService {
        return manifestSyncService ?: synchronized(this) {
            manifestSyncService ?: ManifestSyncService(
                preferenceStore = PreferenceStore(context.applicationContext),
                manifestRepository = provideManifestRepository(context),
                mediaRepository = provideMediaRepository(context),
                storageHelper = MediaStorageHelper(context.applicationContext),
                comparator = provideManifestComparator(),
            ).also { manifestSyncService = it }
        }
    }

    private fun provideDatabase(context: Context): AppDatabase {
        return appDatabase ?: synchronized(this) {
            appDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "playonmytv.db"
            ).fallbackToDestructiveMigration()
                .build()
                .also { appDatabase = it }
        }
    }

    private fun provideManifestRepository(context: Context): ManifestRepository {
        return manifestRepository ?: synchronized(this) {
            manifestRepository ?: ManifestRepository(
                manifestApi = provideManifestApi(),
                mediaDao = provideDatabase(context).mediaDao(),
                syncStateDao = provideDatabase(context).syncStateDao(),
            ).also { manifestRepository = it }
        }
    }

    private fun provideDownloadManager(context: Context): DownloadManager {
        return downloadManager ?: synchronized(this) {
            downloadManager ?: DownloadManager(
                mediaDownloader = MediaDownloader(
                    okHttpClient = provideOkHttpClient(),
                    storageHelper = MediaStorageHelper(context.applicationContext)
                )
            ).also { downloadManager = it }
        }
    }

    private fun provideOkHttpClient(): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build()
                .also { okHttpClient = it }
        }
    }

    private fun provideManifestApi(): ManifestApi {
        return manifestApi ?: synchronized(this) {
            manifestApi ?: ManifestApi(
                okHttpClient = provideOkHttpClient()
            ).also { manifestApi = it }
        }
    }

    private fun provideManifestComparator(): ManifestComparator {
        return manifestComparator ?: synchronized(this) {
            manifestComparator ?: ManifestComparator()
                .also { manifestComparator = it }
        }
    }
}
