# 22 — Roadmap

## Phase 0 — Foundation
- [x] Define features, architecture, tech stack, and full documentation set
- [ ] Set up Android project skeleton (multi-module structure per `05-PROJECT-STRUCTURE.md`)
- [ ] Set up backend: Whisper Large (or faster-whisper) + FastAPI `/transcribe` and `/health` endpoints, behind nginx + TLS + API key auth

## Phase 1 — Core Playback
- [ ] `core/player` module: `PlayerManager` wrapping Media3/ExoPlayer
- [ ] `feature/video`: local video scanning, browsing, playback
- [ ] `feature/audio`: local audio scanning, browsing, playback
- [ ] Base Compose navigation shell (bottom nav: Video / Audio / IPTV / Profile)
- [ ] Room DB schema v1 (`LocalMedia` at minimum)

## Phase 2 — IPTV & Live Streams
- [ ] M3U/M3U8 parser (with malformed-entry tolerance)
- [ ] Xtream Codes API client
- [ ] EPG/XMLTV parsing (optional metadata)
- [ ] `feature/iptv` UI: source management, channel list (IPTV and user-added Live URLs), favorites, search
- [ ] Unified `Channel`/`MediaSource` playback path validated across both

## Phase 3 — AI Subtitles
- [ ] `core/network` backend client (`BackendApiClient`, result wrapper, timeout tuning)
- [ ] Audio extraction (local, one-shot) and rolling buffer capture (IPTV/live)
- [ ] `feature/subtitles`: language picker, generation state machine, cue rendering overlay
- [ ] Local-file subtitle caching (Room `SubtitleCache`)
- [ ] Translation step wired in on the backend for non-English targets (see `11-TRANSLATION-STRATEGY.md`)
- [ ] Error handling & backend-down graceful degradation

## Phase 4 — Polish
- [ ] Full UI/UX pass per `14-UI-UX-DESIGN-SYSTEM.md` (motion, subtitle styling controls, accessibility check)
- [ ] Performance pass: buffer tuning, chunk size tuning, Compose recomposition audit (see `18-PERFORMANCE-OPTIMIZATION.md`)
- [ ] Picture-in-Picture, background/audio-only playback via media session
- [ ] Crash reporting integrated
- [ ] Security checklist review (see `19-SECURITY-PRIVACY.md`)

## Phase 5 — Windows (separate build, later)
- [ ] Decide Windows tech stack independently at that time — no shared codebase assumption carried over from Android
- [ ] Port core feature set: local/IPTV/live playback, subtitle generation via the same backend API (the backend and its API contract are platform-agnostic already, which is the main reusable piece across both platforms)

## Deferred / not currently planned for MVP
- Chromecast/external display casting
- Multi-profile support
- Cloud sync of favorites/history across devices
- On-device offline subtitle fallback (e.g., whisper.cpp) for backend-down scenarios
- Kotlin Multiplatform shared logic (revisit only if Windows work starts soon after Android ships)

## Suggested immediate next step

Once ready to move from docs to code: scaffold the Android project structure (Phase 0/1), and in parallel, stand up the backend `/transcribe` + `/health` endpoints with a real Whisper Large model so the app's networking layer has something real to integrate against early, rather than building the subtitle feature purely against a mocked backend for too long.
