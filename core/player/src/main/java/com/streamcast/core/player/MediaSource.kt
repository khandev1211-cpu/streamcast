package com.streamcast.core.player

import android.net.Uri

/**
 * A unifying model representing any media that can be played in StreamCast.
 * It abstracts away whether the media is a local file, an IPTV channel, or a live URL.
 */
data class MediaSource(
    val id: String,
    val uri: Uri,
    val type: SourceType,
    val displayName: String,
    val isCacheable: Boolean,
    val metadata: SourceMetadata? = null
)

enum class SourceType {
    LOCAL,
    IPTV,
    LIVE_URL
}

/**
 * Metadata for the media source, such as EPG info for IPTV or file duration for local media.
 */
data class SourceMetadata(
    val description: String? = null,
    val duration: Long? = null,
    val thumbnailUrl: String? = null,
    val extras: Map<String, String> = emptyMap()
)
