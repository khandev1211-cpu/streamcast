package com.streamcast.core.database.dao

import androidx.room.*
import com.streamcast.core.database.entities.IptvSource
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvSourceDao {
    @Query("SELECT * FROM IptvSource")
    fun getAll(): Flow<List<IptvSource>>

    @Query("SELECT * FROM IptvSource WHERE id = :id")
    suspend fun getById(id: String): IptvSource?

    @Upsert
    suspend fun upsert(source: IptvSource)

    @Delete
    suspend fun delete(source: IptvSource)
}
