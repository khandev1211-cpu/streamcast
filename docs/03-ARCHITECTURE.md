# 03 — System Architecture

## Architectural style

Layered, modular Android app following a standard **MVVM + Repository** pattern, wrapped in feature modules. This keeps playback, IPTV, live streams, and subtitles as independently testable units that all sit on top of a shared player core.

```
┌─────────────────────────────────────────────┐
│                   UI Layer                    │
│   Jetpack Compose screens + ViewModels        │
├─────────────────────────────────────────────┤
│                Domain Layer                   │
│   Use cases: PlayMedia, GenerateSubtitles,    │
│   ImportPlaylist, AddLiveUrl, etc.            │
├─────────────────────────────────────────────┤
│               Data Layer                      │
│   Repositories: MediaRepository,              │
│   IptvRepository, SubtitleRepository          │
├─────────────────────────────────────────────┤
│         Infrastructure Layer                  │
│   Media3/ExoPlayer, Room DB, Retrofit clients,│
│   File system access                          │
└─────────────────────────────────────────────┘
```

## Module breakdown

```
app/
├── core/
│   ├── player/          # ExoPlayer wrapper, playback state, media session
│   ├── network/         # Retrofit/OkHttp clients: VPS API, IPTV/EPG fetchers
│   └── database/        # Room DB: entities, DAOs, migrations
├── feature/
│   ├── library/         # Local file browsing & playback UI
│   ├── iptv/            # M3U/Xtream parsing, channel list, EPG
│   ├── live/             # User-added live stream URL management
│   └── subtitles/        # Subtitle generation flow, rendering, styling
├── ui/
│   └── theme/            # Compose theme, design tokens, shared components
└── settings/              # Preferences, VPS endpoint config, subtitle defaults
```

## Why this structure

- **Feature modules map to the three content types plus subtitles** — this mirrors the product's own mental model and keeps each subsystem's complexity contained (IPTV parsing quirks don't leak into local playback code, etc).
- **`core/player` is the single source of truth for playback** — every feature module calls into it rather than instantiating its own player instance, so playback state (position, buffering, errors) is consistent regardless of source.
- **Repositories abstract data origin from the UI** — a `MediaRepository` might pull from Room (local library) or a network call (IPTV channel metadata); the ViewModel doesn't need to know which.

## The unifying concept: `MediaSource`

All three content types converge into a single internal model before reaching the player:

```kotlin
data class MediaSource(
    val id: String,
    val uri: String,
    val type: SourceType,       // LOCAL, IPTV, LIVE_URL
    val displayName: String,
    val isCacheable: Boolean,   // true for LOCAL, false for IPTV/LIVE
    val metadata: SourceMetadata? // EPG info, duration, etc — nullable, source-dependent
)
```

This is what lets the subtitle system, the player, and the UI treat "a local movie," "an IPTV channel," and "a pasted live URL" identically wherever possible, differing only where they genuinely must (caching behavior, EPG availability).

## Data flow: end-to-end subtitle generation

```
User taps "Generate Subtitles" on any playing MediaSource
        │
        ▼
SubtitleViewModel invokes GenerateSubtitlesUseCase
        │
        ▼
Audio extracted/captured (full track for LOCAL, rolling buffer for IPTV/LIVE)
        │
        ▼
SubtitleRepository sends chunk + target language to VPS via Retrofit client
        │
        ▼
VPS (Whisper Large + translation step) returns timed text segments
        │
        ▼
Segments converted to renderable subtitle cues
        │
        ▼
Player overlays cues synced to playback position
        │
        ▼
(LOCAL only) SubtitleRepository caches result in Room, keyed by media id + language
```

## Cross-cutting concerns

- **Error handling**: every network call (VPS, IPTV fetch, EPG fetch) goes through a shared result-wrapper pattern (see `17-ERROR-HANDLING-LOGGING.md`) so failures surface consistently in the UI.
- **Threading**: Kotlin Coroutines + Flow throughout; playback state and subtitle segments are exposed as `StateFlow`/`Flow` to the UI layer.
- **Configuration**: the VPS endpoint is not hardcoded — it lives in a settings-backed config so it can be changed without a rebuild.

See `05-PROJECT-STRUCTURE.md` for the actual file/package layout, and `16-STATE-MANAGEMENT.md` for how state flows through the UI layer in detail.
