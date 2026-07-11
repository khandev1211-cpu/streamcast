package com.streamcast.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = IptvSource::class,
        parentColumns = ["id"],
        childColumns = ["sourceId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Channel(
    @PrimaryKey val id: String,
    val sourceId: String,
    val name: String,
    val logoUrl: String?,
    val category: String?,
    val country: String?,
    val streamUrl: String,
    val epgChannelId: String?,
    val isFavorite: Boolean = false
)
