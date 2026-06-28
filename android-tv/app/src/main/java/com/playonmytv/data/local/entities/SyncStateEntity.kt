package com.playonmytv.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val manifestVersion: Long = 0,
    val lastSync: Long?,
    val lastSuccess: Long?,
    val lastFailure: Long?,
    val lastError: String?,
)
