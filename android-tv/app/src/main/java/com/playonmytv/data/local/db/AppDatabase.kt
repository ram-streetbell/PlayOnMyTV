package com.playonmytv.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.playonmytv.data.local.dao.DeviceConfigDao
import com.playonmytv.data.local.dao.LocalMediaDao
import com.playonmytv.data.local.dao.LocalPlaylistDao
import com.playonmytv.data.local.dao.LocalScheduleDao
import com.playonmytv.data.local.dao.MediaDao
import com.playonmytv.data.local.dao.SyncStateDao
import com.playonmytv.data.local.entities.DeviceConfigEntity
import com.playonmytv.data.local.entities.LocalMediaEntity
import com.playonmytv.data.local.entities.LocalPlaylistEntity
import com.playonmytv.data.local.entities.LocalPlaylistItemEntity
import com.playonmytv.data.local.entities.LocalScheduleEntity
import com.playonmytv.data.local.entities.LocalScheduleSlotEntity
import com.playonmytv.data.local.entities.MediaEntity
import com.playonmytv.data.local.entities.SyncStateEntity

@Database(
    entities = [
        DeviceConfigEntity::class,
        LocalMediaEntity::class,
        LocalPlaylistEntity::class,
        LocalPlaylistItemEntity::class,
        LocalScheduleEntity::class,
        LocalScheduleSlotEntity::class,
        MediaEntity::class,
        SyncStateEntity::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceConfigDao(): DeviceConfigDao
    abstract fun localMediaDao(): LocalMediaDao
    abstract fun localPlaylistDao(): LocalPlaylistDao
    abstract fun localScheduleDao(): LocalScheduleDao
    abstract fun mediaDao(): MediaDao
    abstract fun syncStateDao(): SyncStateDao
}
