package com.streamcast.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.streamcast.core.database.entities.SubtitleCache

@Dao
interface SubtitleCacheDao {
    @Query("SELECT * FROM SubtitleCache WHERE mediaId = :mediaId AND language = :language")
    suspend fun get(mediaId: String, language: String): SubtitleCache?

    @Upsert
    suspend fun upsert(cache: SubtitleCache)
}
