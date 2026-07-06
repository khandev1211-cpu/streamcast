package com.streamcast.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.streamcast.core.database.dao.ChannelDao
import com.streamcast.core.database.dao.SubtitleCacheDao
import com.streamcast.core.database.entities.Channel
import com.streamcast.core.database.entities.IptvSource
import com.streamcast.core.database.entities.LocalMedia
import com.streamcast.core.database.entities.SubtitleCache

@Database(
    entities = [
        LocalMedia::class,
        IptvSource::class,
        Channel::class,
        SubtitleCache::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun subtitleCacheDao(): SubtitleCacheDao
}
