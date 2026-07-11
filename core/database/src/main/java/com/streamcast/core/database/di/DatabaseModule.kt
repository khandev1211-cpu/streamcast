package com.streamcast.core.database.di

import android.content.Context
import androidx.room.Room
import com.streamcast.core.database.AppDatabase
import com.streamcast.core.database.dao.ChannelDao
import com.streamcast.core.database.dao.EpgDao
import com.streamcast.core.database.dao.IptvSourceDao
import com.streamcast.core.database.dao.LocalMediaDao
import com.streamcast.core.database.dao.SubtitleCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "streamcast-db"
        ).fallbackToDestructiveMigration() // Destructive for early dev as per docs
            .build()
    }

    @Provides
    fun provideChannelDao(db: AppDatabase): ChannelDao = db.channelDao()

    @Provides
    fun provideEpgDao(db: AppDatabase): EpgDao = db.epgDao()

    @Provides
    fun provideLocalMediaDao(db: AppDatabase): LocalMediaDao = db.localMediaDao()

    @Provides
    fun provideIptvSourceDao(db: AppDatabase): IptvSourceDao = db.iptvSourceDao()

    @Provides
    fun provideSubtitleCacheDao(db: AppDatabase): SubtitleCacheDao = db.subtitleCacheDao()
}
