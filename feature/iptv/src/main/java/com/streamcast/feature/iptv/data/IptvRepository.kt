package com.streamcast.feature.iptv.data

import com.streamcast.core.database.dao.ChannelDao
import com.streamcast.core.database.dao.IptvSourceDao
import com.streamcast.core.database.entities.Channel
import com.streamcast.core.database.entities.IptvSource
import com.streamcast.core.network.IptvApiClient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import com.streamcast.feature.iptv.parser.M3UParser
import java.io.File

@Singleton
class IptvRepository @Inject constructor(
    private val sourceDao: IptvSourceDao,
    private val channelDao: ChannelDao,
    private val apiClient: IptvApiClient
) {
    fun getSources(): Flow<List<IptvSource>> = sourceDao.getAll()
    
    fun getChannelsForSource(sourceId: String): Flow<List<Channel>> = 
        channelDao.getChannelsForSource(sourceId)

    fun getAllChannels(): Flow<List<Channel>> = channelDao.getAllChannels()

    suspend fun updateFavorite(channelId: String, isFavorite: Boolean) {
        channelDao.updateFavorite(channelId, isFavorite)
    }

    suspend fun checkChannelHealth(channel: Channel) {
        try {
            val response = apiClient.fetchRawPlaylist(channel.streamUrl) // Use the same call or create a HEAD one
            val status = if (response.isNotEmpty()) 1 else 2
            channelDao.upsertAll(listOf(channel.copy(lastCheckStatus = status)))
        } catch (e: Exception) {
            channelDao.upsertAll(listOf(channel.copy(lastCheckStatus = 2)))
        }
    }

    suspend fun addSource(source: IptvSource) {
        sourceDao.upsert(source)
    }

    suspend fun addChannel(channel: Channel) {
        channelDao.upsertAll(listOf(channel))
    }

    suspend fun refreshSource(sourceId: String) {
        val source = sourceDao.getById(sourceId) ?: return
        when (source.type) {
            "XTREAM" -> {
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
                                country = null,
                                streamUrl = "${source.host}/live/${source.username}/${source.password}/${stream.stream_id}.m3u8",
                                epgChannelId = null
                            )
                        )
                    }
                }
                channelDao.upsertAll(channels)
            }
            "M3U_LOCAL" -> {
                source.playlistUrl?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val content = file.readText()
                        val country = if (file.name.endsWith(".m3u")) file.nameWithoutExtension else null
                        val channels = M3UParser.parse(content, source.id, defaultCountry = country)
                        channelDao.upsertAll(channels)
                    }
                }
            }
            "M3U_REMOTE" -> {
                source.playlistUrl?.let { url ->
                    try {
                        val content = apiClient.fetchRawPlaylist(url)
                        val channels = M3UParser.parse(content, source.id)
                        channelDao.upsertAll(channels)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    suspend fun importFromDirectory(directoryPath: String) {
        val dir = File(directoryPath)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles { _, name -> name.endsWith(".m3u") || name.endsWith(".m3u8") }?.forEach { file ->
                val source = IptvSource(
                    id = "local_${file.nameWithoutExtension}",
                    name = "Local: ${file.nameWithoutExtension}",
                    type = "M3U_LOCAL",
                    playlistUrl = file.absolutePath,
                    host = null,
                    username = null,
                    password = null,
                    lastSyncedAt = null
                )
                addSource(source)
                refreshSource(source.id)
            }
        }
    }
}
