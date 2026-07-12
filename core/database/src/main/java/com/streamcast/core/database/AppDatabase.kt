package com.streamcast.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.streamcast.core.database.dao.ChannelDao
import com.streamcast.core.database.dao.EpgDao
import com.streamcast.core.database.dao.IptvSourceDao
import com.streamcast.core.database.dao.LocalMediaDao
import com.streamcast.core.database.dao.SubtitleCacheDao
import com.streamcast.core.database.entities.Channel
import com.streamcast.core.database.entities.EpgProgram
import com.streamcast.core.database.entities.IptvSource
import com.streamcast.core.database.entities.LocalMedia
import com.streamcast.core.database.entities.SubtitleCache

@Database(
    entities = [
        LocalMedia::class,
        IptvSource::class,
        Channel::class,
        SubtitleCache::class,
        EpgProgram::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao
    abstract fun localMediaDao(): LocalMediaDao
    abstract fun iptvSourceDao(): IptvSourceDao
    abstract fun subtitleCacheDao(): SubtitleCacheDao
}
