package com.streamcast.core.database.entities

import androidx.room.Entity

@Entity(primaryKeys = ["mediaId", "language"])
data class SubtitleCache(
    val mediaId: String,
    val language: String,      // e.g., "es", "ur"
    val cuesJson: String,      // serialized list of {start, end, text}
    val generatedAt: Long
)
