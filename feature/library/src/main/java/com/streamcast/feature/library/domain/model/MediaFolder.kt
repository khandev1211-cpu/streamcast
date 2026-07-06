package com.streamcast.feature.library.domain.model

import com.streamcast.core.player.MediaSource

data class MediaFolder(
    val name: String,
    val path: String,
    val mediaCount: Int,
    val items: List<MediaSource> = emptyList()
)
