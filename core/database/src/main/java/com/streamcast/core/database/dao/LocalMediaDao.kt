package com.streamcast.core.database.dao

import androidx.room.*
import com.streamcast.core.database.entities.LocalMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalMediaDao {
    @Query("SELECT * FROM LocalMedia")
    fun getAll(): Flow<List<LocalMedia>>

    @Query("SELECT * FROM LocalMedia WHERE id = :id")
    suspend fun getById(id: String): LocalMedia?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: LocalMedia)

    @Update
    suspend fun update(media: LocalMedia)

    @Query("UPDATE LocalMedia SET lastPositionMs = :positionMs, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun updatePosition(id: String, positionMs: Long, timestamp: Long)

    @Delete
    suspend fun delete(media: LocalMedia)
}
