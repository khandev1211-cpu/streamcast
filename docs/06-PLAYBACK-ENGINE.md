# 06 — Playback Engine

## Why Media3 (ExoPlayer)

Media3 is Google's actively maintained media playback library and the de facto standard for Android apps that need more than the built-in `MediaPlayer` API. It's chosen here because it handles all three of this app's content types through one engine:

- **Local files** — broad container/codec support (MP4, MKV, WebM, MP3, FLAC, etc., subject to device codec availability).
- **HLS (.m3u8)** — native support, used by most IPTV providers and many live streams.
- **DASH (.mpd)** — native support, used by some live streaming setups.
- **RTMP** — supported via an extension library (not in Media3 core), relevant for some user-pasted live URLs.

## PlayerManager responsibilities

A single `PlayerManager` (in `core/player`) wraps the actual `ExoPlayer` instance and is the only thing feature modules talk to. It exposes:

- `play(mediaSource: MediaSource)` — prepares and starts playback for any source type.
- `pause()`, `resume()`, `seekTo(positionMs)`, `setPlaybackSpeed(speed)`.
- `playbackState: StateFlow<PlaybackState>` — buffering, playing, paused, ended, error — so any screen can observe current state without owning the player.
- `currentPositionMs: StateFlow<Long>` — for scrubbers/progress bars, and critically, for the subtitle renderer to know what position to sync cues against.

## Handling the three source types uniformly

Because every source becomes a `MediaSource` with a `uri` and a `type` before reaching the player (see `03-ARCHITECTURE.md`), `PlayerManager` largely doesn't branch on type — it hands the URI to ExoPlayer's `MediaItem` builder, which auto-detects the format in most cases. Where explicit handling is needed:

- **RTMP URLs** require the RTMP extension's `MediaSource.Factory` rather than the default one — `PlayerManager` checks the URI scheme (`rtmp://`) to pick the right factory.
- **IPTV channels with EPG data** attach extra metadata (`SourceMetadata`) that the player ignores but the UI layer (now/next program info) uses.

## Resume & history (local files only)

- On `pause()` or app backgrounding, `PlayerManager` emits the current position; `LibraryRepository` persists it against the `LocalMedia` entity in Room.
- On next play of that file, playback seeks to the saved position automatically (with a "Resume from X" prompt as a nicer UX option rather than silently jumping).
- Not applicable to IPTV/live sources — there's no meaningful "resume position" for a live channel.

## Buffering & network resilience (IPTV/live)

- Live sources are more prone to buffering stalls and connection drops than local files. `PlayerManager` surfaces distinct states (`BUFFERING`, `RECONNECTING`, `ERROR`) so the UI can show relevant messaging instead of a generic spinner.
- A basic retry policy (e.g., a few automatic reconnect attempts with backoff) is reasonable for live streams before surfacing a hard error to the user.

## Audio extraction for subtitles

The subtitle feature needs access to the audio track:

- **Local files**: audio can be extracted directly from the file (e.g., via `MediaExtractor` or by having the VPS accept the file/audio directly).
- **IPTV/live**: since there's no fixed file, the app captures a rolling buffer of decoded/raw audio during playback to send in chunks. This is handled by a small audio-tap component that sits alongside playback rather than inside `PlayerManager` itself, so the player's core responsibilities stay focused on playback, not subtitle logistics. See `09-SUBTITLE-PIPELINE.md` for the full flow.

## Picture-in-Picture & background audio (later phase)

- PiP requires Activity-level support (`enterPictureInPictureMode`) and isn't part of Phase 1 scope, but `PlayerManager`'s state-based design means it should slot in later without restructuring playback logic.
- Background/audio-only mode (screen off, audio continues) uses a foreground `MediaSessionService` — worth scaffolding early even if not fully polished in Phase 1, since retrofitting a media session later is more disruptive than including a basic one from the start.
