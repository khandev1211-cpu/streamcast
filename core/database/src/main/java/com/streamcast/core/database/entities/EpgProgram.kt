package com.streamcast.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EpgProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String, // Matches Channel.epgChannelId
    val title: String,
    val description: String?,
    val startTime: Long, // Epoch millis
    val endTime: Long
)
