package com.streamcast.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LocalMedia(
    @PrimaryKey val id: String,
    val filePath: String,
    val displayName: String,
    val durationMs: Long?,
    val lastPositionMs: Long = 0,
    val lastPlayedAt: Long? = null,
    val addedAt: Long
)
