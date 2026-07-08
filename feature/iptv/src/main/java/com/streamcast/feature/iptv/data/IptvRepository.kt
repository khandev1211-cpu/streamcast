package com.streamcast.feature.iptv.data

import com.streamcast.core.database.dao.ChannelDao
import com.streamcast.core.database.dao.IptvSourceDao
import com.streamcast.core.database.entities.Channel
import com.streamcast.core.database.entities.IptvSource
import com.streamcast.core.network.IptvApiClient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvRepository @Inject constructor(
    private val sourceDao: IptvSourceDao,
    private val channelDao: ChannelDao,
    private val apiClient: IptvApiClient
) {
    fun getSources(): Flow<List<IptvSource>> = sourceDao.getAll()
    
    fun getChannelsForSource(sourceId: String): Flow<List<Channel>> = 
        channelDao.getChannelsForSource(sourceId)

    suspend fun addSource(source: IptvSource) {
        sourceDao.upsert(source)
    }

    suspend fun refreshSource(sourceId: String) {
        val source = sourceDao.getById(sourceId) ?: return
        if (source.type == "XTREAM") {
            // Xtream Codes Refresh
            val categories = apiClient.getLiveCategories(source.username!!, source.password!!)
            val channels = mutableListOf<Channel>()
            categories.forEach { category ->
                val streams = apiClient.getLiveStreams(source.username!!, source.password!!, categoryId = category.category_id)
                streams.forEach { stream ->
                    channels.add(
                        Channel(
                            id = "${source.id}_${stream.stream_id}",
                            sourceId = source.id,
                            name = stream.name,
                            logoUrl = stream.stream_icon,
                            category = category.category_name,
                            streamUrl = "${source.host}/live/${source.username}/${source.password}/${stream.stream_id}.m3u8",
                            epgChannelId = null // TODO
                        )
                    )
                }
            }
            channelDao.upsertAll(channels)
        }
    }
}
