package com.streamcast.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.streamcast.core.database.entities.EpgProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Query("SELECT * FROM EpgProgram WHERE channelId = :channelId AND endTime > :currentTime ORDER BY startTime ASC LIMIT 2")
    fun getCurrentAndNext(channelId: String, currentTime: Long): Flow<List<EpgProgram>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<EpgProgram>)

    @Query("DELETE FROM EpgProgram WHERE endTime < :currentTime")
    suspend fun deleteOldPrograms(currentTime: Long)
}
