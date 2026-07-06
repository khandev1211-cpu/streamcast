# 12 — Database Schema (Room)

## Entities

### `LocalMedia`
Represents a scanned or manually-added local video/audio file.

```kotlin
@Entity
data class LocalMedia(
    @PrimaryKey val id: String,
    val filePath: String,
    val displayName: String,
    val durationMs: Long?,
    val lastPositionMs: Long = 0,
    val lastPlayedAt: Long? = null,
    val addedAt: Long
)
```

### `IptvSource`
Represents one IPTV provider login or imported playlist.

```kotlin
@Entity
data class IptvSource(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,           // "M3U" or "XTREAM"
    val playlistUrl: String?,   // for M3U type
    val host: String?,          // for XTREAM type
    val username: String?,      // encrypted at rest — see 19-SECURITY-PRIVACY.md
    val password: String?,      // encrypted at rest
    val lastSyncedAt: Long?
)
```

### `Channel`
Represents a single channel — from IPTV, or a user-added live URL (via a synthetic `IptvSource` of type `"USER_ADDED"`).

```kotlin
@Entity(
    foreignKeys = [ForeignKey(
        entity = IptvSource::class,
        parentColumns = ["id"],
        childColumns = ["sourceId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Channel(
    @PrimaryKey val id: String,
    val sourceId: String,
    val name: String,
    val logoUrl: String?,
    val category: String?,
    val streamUrl: String,
    val epgChannelId: String?,
    val isFavorite: Boolean = false
)
```

### `SubtitleCache`
Cached subtitle results — local files only (IPTV/live subtitles aren't cached, see `09-SUBTITLE-PIPELINE.md`).

```kotlin
@Entity(primaryKeys = ["mediaId", "language"])
data class SubtitleCache(
    val mediaId: String,       // references LocalMedia.id
    val language: String,      // e.g., "es", "ur"
    val cuesJson: String,      // serialized list of {start, end, text}
    val generatedAt: Long
)
```

### `PlaybackHistory` (optional, could also be inferred from `LocalMedia.lastPlayedAt`)
If richer history (multiple past sessions, not just "last played") is wanted later:

```kotlin
@Entity
data class PlaybackHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: String,
    val mediaType: String,    // "LOCAL", "IPTV", "LIVE"
    val playedAt: Long,
    val positionMs: Long
)
```

## Relationships

```
IptvSource 1 ──── * Channel
LocalMedia 1 ──── * SubtitleCache   (via mediaId, language composite key)
```

Channels don't reference `LocalMedia` — the two are entirely separate concepts (a `Channel` is always network-sourced/live in nature, per this app's model).

## DAOs (illustrative, not exhaustive)

```kotlin
@Dao
interface ChannelDao {
    @Query("SELECT * FROM Channel WHERE sourceId = :sourceId")
    fun getChannelsForSource(sourceId: String): Flow<List<Channel>>

    @Query("SELECT * FROM Channel WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<Channel>>

    @Upsert
    suspend fun upsertAll(channels: List<Channel>)
}

@Dao
interface SubtitleCacheDao {
    @Query("SELECT * FROM SubtitleCache WHERE mediaId = :mediaId AND language = :language")
    suspend fun get(mediaId: String, language: String): SubtitleCache?

    @Upsert
    suspend fun upsert(cache: SubtitleCache)
}
```

## Migration strategy

- Room migrations should be written explicitly (not relying on `fallbackToDestructiveMigration`) once the app has real users, since destructive migrations would wipe local libraries, favorites, and cached subtitles.
- During early development (pre-release), destructive migration is acceptable to move faster — but this should be revisited before any real release build.

## Storage size considerations

- `SubtitleCache.cuesJson` for a full movie could be a non-trivial amount of text (thousands of timed segments) — acceptable for SQLite/Room, but worth periodically letting users clear old/unused cache entries from Settings (see `02-FEATURES.md`) rather than growing unbounded.
