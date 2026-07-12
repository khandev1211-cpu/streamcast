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

    fun getFavorites(): Flow<List<Channel>> = channelDao.getFavorites()
    
    fun getRecents(): Flow<List<Channel>> = channelDao.getRecents()

    suspend fun updateFavorite(channelId: String, isFavorite: Boolean) {
        channelDao.updateFavorite(channelId, isFavorite)
    }

    suspend fun updateLastPlayed(channelId: String) {
        channelDao.updateLastPlayed(channelId, System.currentTimeMillis())
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
                val username = source.username!!
                val password = source.password!!
                val channels = mutableListOf<Channel>()

                // 1. Refresh Live TV
                val liveCategories = apiClient.getLiveCategories(username, password)
                liveCategories.forEach { category ->
                    val streams = apiClient.getLiveStreams(username, password, categoryId = category.category_id)
                    streams.forEach { stream ->
                        channels.add(
                            Channel(
                                id = "${source.id}_live_${stream.stream_id}",
                                sourceId = source.id,
                                name = stream.name,
                                logoUrl = stream.stream_icon,
                                category = category.category_name,
                                country = null,
                                streamUrl = "${source.host}/live/$username/$password/${stream.stream_id}.m3u8",
                                epgChannelId = null
                            )
                        )
                    }
                }

                // 2. Refresh VOD (Movies)
                val vodCategories = apiClient.getVodCategories(username, password)
                vodCategories.forEach { category ->
                    val streams = apiClient.getVodStreams(username, password, categoryId = category.category_id)
                    streams.forEach { stream ->
                        channels.add(
                            Channel(
                                id = "${source.id}_vod_${stream.stream_id}",
                                sourceId = source.id,
                                name = stream.name,
                                logoUrl = stream.stream_icon,
                                category = "Movies: ${category.category_name}",
                                country = null,
                                streamUrl = "${source.host}/movie/$username/$password/${stream.stream_id}.mp4", // Typical Xtream VOD format
                                epgChannelId = null
                            )
                        )
                    }
                }

                // 3. Refresh Series
                val seriesCategories = apiClient.getSeriesCategories(username, password)
                seriesCategories.forEach { category ->
                    val seriesList = apiClient.getSeries(username, password, categoryId = category.category_id)
                    seriesList.forEach { s ->
                        channels.add(
                            Channel(
                                id = "${source.id}_series_${s.stream_id}",
                                sourceId = source.id,
                                name = s.name,
                                logoUrl = s.stream_icon,
                                category = "Series: ${category.category_name}",
                                country = null,
                                streamUrl = "${source.host}/series/$username/$password/${s.stream_id}.m3u8", // Series usually points to episodes/m3u8
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
                        // Try to guess country from URL (e.g., .../countries/in.m3u)
                        val country = if (url.contains("/countries/")) {
                            url.substringAfterLast("/").substringBefore(".m3u")
                        } else null
                        
                        val channels = M3UParser.parse(content, source.id, defaultCountry = country)
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
