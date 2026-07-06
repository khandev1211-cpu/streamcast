package com.streamcast.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class IptvSource(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,           // "M3U" or "XTREAM"
    val playlistUrl: String?,   // for M3U type
    val host: String?,          // for XTREAM type
    val username: String?,      // encrypted at rest
    val password: String?,      // encrypted at rest
    val lastSyncedAt: Long?
)
