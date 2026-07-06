# 02 — Feature Specification

## Playback (all sources)

- Play video and audio from local storage, IPTV streams, or arbitrary live URLs through a single player UI.
- Standard controls: play/pause, seek (with scrub preview if feasible), volume and brightness gestures, playback speed (0.5x–2x), lock screen for accidental-touch prevention.
- Resume playback — remember position per local file; not applicable to live content.
- Background/audio-only mode — keep audio playing when the screen is off or the app is backgrounded (useful for music files and radio-style IPTV audio channels).
- Picture-in-picture (later phase) for multitasking.

## Local Media Library

- Scan device storage for supported video/audio files (respecting Android scoped storage rules).
- Organize by folder and/or by media type (video vs audio).
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
- Choose a target language from a supported list; the app requests transcription (and translation if the target isn't the spoken language) from the developer's self-hosted Whisper Large API.
- Subtitles render as an overlay, synced to playback position, with adjustable size/color/background.
- **Local files**: subtitle results are cached (tied to that file + chosen language) so regeneration isn't needed on replay.
- **IPTV/live streams**: subtitles are generated in a rolling near-real-time fashion since the content isn't fixed; not cached.
- Clear UI feedback during generation (loading state, error state if the VPS is unreachable or slow).

## Settings

- VPS/API endpoint configuration (since it's self-hosted, the developer may need to change the URL).
- Default subtitle language and style preferences.
- Storage/cache management (clear cached subtitles, clear playback history).
- Theme (dark-first, possibly light mode later).

## Explicitly deferred to later phases

- Chromecast/external display casting.
- Multi-profile support.
- Cloud sync of favorites/history across devices.
- On-device offline subtitle fallback model (e.g., whisper.cpp) for when the VPS is unreachable.
- Windows native app.

## Explicitly out of scope

- The app is a player/client only — it does not bundle, promote, or source any specific IPTV provider or content. Users supply their own playlists/credentials/URLs.
- No DRM circumvention — DRM-protected streams are only playable if properly licensed via the source itself.
