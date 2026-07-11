package com.streamcast.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.streamcast.core.database.entities.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM Channel WHERE sourceId = :sourceId")
    fun getChannelsForSource(sourceId: String): Flow<List<Channel>>

    @Query("SELECT * FROM Channel")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM Channel WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<Channel>>

    @Upsert
    suspend fun upsertAll(channels: List<Channel>)

    @Query("UPDATE Channel SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)
}
