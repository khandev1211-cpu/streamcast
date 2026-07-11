package com.streamcast.feature.iptv.data

import com.streamcast.core.player.MediaSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvPlaylistManager @Inject constructor() {
    private var activePlaylist: List<MediaSource> = emptyList()

    fun setPlaylist(playlist: List<MediaSource>) {
        activePlaylist = playlist
    }

    fun getPlaylist(): List<MediaSource> = activePlaylist
}
