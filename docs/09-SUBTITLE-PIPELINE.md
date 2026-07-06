# 09 — AI Subtitle Pipeline

This is the app's core differentiator, so it's covered in depth here, with the VPS-specific setup broken out further into `10-VPS-WHISPER-SETUP.md` and `11-TRANSLATION-STRATEGY.md`.

## Goal

Let a user generate subtitles, in a language of their choosing, for whatever is currently playing — local file, IPTV channel, or user-added live stream — using a Whisper Large model the developer runs on their own VPS.

## High-level flow

```
User taps "Generate Subtitles" on the current video
        │
        ▼
Pick target language (dropdown)
        │
        ▼
Audio obtained:
  - LOCAL: full/extracted audio track
  - IPTV/LIVE: rolling buffer (e.g., last 20–30s)
        │
        ▼
Sent to VPS /transcribe endpoint (chunked for live sources)
        │
        ▼
VPS: Whisper Large transcribes → (if needed) translates → returns timed segments
        │
        ▼
App converts segments into subtitle cues
        │
        ▼
Renderer overlays cues synced to current playback position
        │
        ▼
(LOCAL only) Cache result keyed by (media id, language)
```

## Two distinct modes

### One-shot mode (local files)
- The whole audio track (or the file itself) is sent once.
- Full segment list comes back, converted to an SRT/WebVTT-like in-memory structure.
- Cached in Room (`SubtitleCache` entity) so replaying the same file in the same language doesn't re-trigger generation.
- If the user picks a different language for a file that already has cached subtitles in another language, that's a separate cache entry — not a conflict.

### Rolling mode (IPTV / live streams)
- No fixed file to send — the app maintains a rolling buffer of recent audio (a sliding window, e.g., 20–30 seconds) and periodically sends the newest chunk.
- Segments come back with a small inherent delay (network + inference time + buffer window) — subtitles for live content are "near real-time," not instantaneous, and the UI should not imply otherwise.
- Nothing is cached — every viewing session regenerates from scratch since live content isn't fixed.

## Client-side responsibilities

- **Audio capture**: for local files, extracting the audio track; for live sources, tapping the decoded audio stream into a rolling buffer without disrupting playback.
- **Request management**: chunking, sending, and handling responses asynchronously so subtitle generation never blocks playback itself.
- **Cue conversion**: turning `{start, end, text}` segments from the VPS into a renderable subtitle cue list, with interpolation/timing adjustment if there's slight drift between segment timestamps and actual playback position.
- **Rendering**: an overlay component that reads current playback position (from `PlayerManager`) and displays whichever cue's time range contains that position.
- **Caching**: for local-file mode, writing results to Room and checking the cache before making a new request.

## UX requirements

- Visible loading/progress state while the first segment is being generated — this can take a few seconds depending on VPS load, and users need to know it's working, not stuck.
- Ability to toggle subtitles off instantly, client-side, without needing to notify or stop anything server-side (the client just stops requesting/rendering).
- Clear, specific error messaging if the VPS is unreachable, times out, or returns an error — distinguishing "can't reach your server" from "server returned an error" helps the developer debug their own VPS setup too.
- A language picker that's simple to use even with a long list of supported languages (searchable dropdown rather than a long static list).

## Open decisions to make before implementation

- **Rolling buffer size** for live sources — trade-off between subtitle latency (smaller buffer = faster but more requests/VPS load) and transcription accuracy (Whisper tends to do better with more context per chunk).
- **Retry/backoff policy** if a VPS request fails mid-stream for live subtitles — silently retry the next chunk, or surface an error immediately?
- **Rate limiting** — even for a single-user personal app, it's worth deciding whether the VPS should reject overlapping requests (e.g., if the user rapidly toggles subtitles on/off) rather than queuing up redundant work.

See `10-VPS-WHISPER-SETUP.md` for the backend side and `13-NETWORKING-API-CONTRACTS.md` for the exact request/response shapes.
