# 02 — Feature Specification

## Playback (all sources)

- Play video and audio from local storage, IPTV streams, or arbitrary live URLs through a single player UI.
- Standard controls: play/pause, seek (with scrub preview), volume and brightness gestures, playback speed (0.5x–3x), lock screen for accidental-touch prevention.
- **MX-Player-style gesture controls**: swipe left half for brightness, right half for volume, double-tap left/right to seek ±10s, pinch/drag to zoom or resize video, long-press for temporary speed boost. Full spec in `25-MX-PLAYER-FEATURE-PARITY.md`.
- Aspect ratio / zoom modes (fit, fill, crop, stretch).
- Resume playback — remember position per local file, with a "Resume from X?" prompt; not applicable to live content.
- Background/audio-only mode — keep audio playing when the screen is off or the app is backgrounded (useful for music files and radio-style IPTV audio channels).
- A-B repeat (loop a section) — pairs well with AI subtitles for re-watching a phrase while learning a language.
- Sleep timer — stop playback after a set duration.
- Basic equalizer / audio gain boost for quiet files.
- Hardware/software decoder toggle for device compatibility troubleshooting.
- Multiple audio track and subtitle track selection (embedded + external), shown alongside the AI-generated subtitle option in one unified picker.
- Subtitle sync offset adjustment (+/- milliseconds) for both external and AI-generated subtitles.
- Picture-in-picture and floating/pop-up window mode (later phase) for multitasking.

## Local Media Library

- Scan device storage for supported video/audio files (respecting Android scoped storage rules).
- **Folder-first grid browsing with thumbnails**, MX-Player-style, plus a list-view toggle.
- Sort options (name, date added, size, duration).
- "Last played" quick-resume row surfaced at the top of the library.
- Long-press multi-select for batch actions (delete from library, add to favorites).
- Private/locked folders — passcode-gated, hidden from the main grid.
- Manual "add file" via system file picker for files outside scanned folders.
- Playback history — last watched, last position.
- Basic metadata display: filename, duration, resolution where available.

## IPTV

- Add a source via M3U/M3U8 playlist URL, or upload a local `.m3u` file.
- Xtream Codes login (host, username, password) — a common IPTV provider authentication format; the app fetches the channel/category list via their API.
- Channel list grouped by category (as defined in the playlist/provider).
- EPG (Electronic Program Guide) — parse XMLTV data where the provider supplies it, show "now/next" program info per channel.
- Favorites and search across channels.
- Multiple IPTV sources manageable at once (e.g., different providers or playlists).

## Live Streams (user-added)

- Paste any HLS (`.m3u8`), DASH (`.mpd`), or RTMP URL and play it directly, no provider login needed.
- Save pasted URLs as custom "channels" the user can return to, alongside IPTV ones.
- Basic reachability/health check before saving a URL (attempt a quick connection/parse to catch obviously broken links early).

## AI Subtitle Generation — the core feature

- Trigger subtitle generation for **any** currently playing video, from any source.
- Choose a target language from a supported list; the app requests transcription (and translation if the target isn't the spoken language) from the Whisper-powered backend.
- Subtitles render as an overlay, synced to playback position, with adjustable size/color/background.
- **Local files**: subtitle results are cached (tied to that file + chosen language) so regeneration isn't needed on replay.
- **IPTV/live streams**: subtitles are generated in a rolling near-real-time fashion since the content isn't fixed; not cached.
- Clear UI feedback during generation (loading state, error state if the backend is unreachable or slow).

## Settings

- Backend API endpoint configuration.
- Default subtitle language and style preferences.
- Storage/cache management (clear cached subtitles, clear playback history).
- Theme (dark-first, possibly light mode later).

## Explicitly deferred to later phases

- Chromecast/external display casting.
- Multi-profile support.
- Cloud sync of favorites/history across devices.
- On-device offline subtitle fallback model (e.g., whisper.cpp) for when the backend is unreachable.
- Windows native app.

## Explicitly out of scope

- The app is a player/client only — it does not bundle, promote, or source any specific IPTV provider or content. Users supply their own playlists/credentials/URLs.
- No DRM circumvention — DRM-protected streams are only playable if properly licensed via the source itself.
