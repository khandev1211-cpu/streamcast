# 22 — Roadmap

## Phase 0 — Foundation
- [x] Define features, architecture, tech stack, and full documentation set
- [x] Set up Android project skeleton (multi-module structure)
- [ ] Set up VPS: Whisper Large (or faster-whisper) + FastAPI `/transcribe` and `/health` endpoints

## Phase 1 — Core Playback (MX Player Style)
- [x] `core/player` module: `PlayerManager` wrapping Media3/ExoPlayer
- [x] `feature/library`: local file scanning, folder/subfolder navigation
- [x] Advanced Player UI: Gestures (Vol/Bri/Seek), Pinch-to-zoom, Decoder toggle, Speed control
- [x] Base Compose navigation (Bottom nav)
- [x] Room DB schema v1

## Phase 2 — IPTV & Live Streams
- [ ] M3U/M3U8 parser (with malformed-entry tolerance)
- [ ] Xtream Codes API client
- [ ] EPG/XMLTV parsing (optional metadata)
- [ ] `feature/iptv` UI: source management, channel list, favorites, search
- [ ] `feature/live` UI: add/manage user-pasted live URLs

## Phase 3 — AI Subtitles (Flagship Feature)
- [ ] `core/network` VPS client (`VpsApiClient`, result wrapper)
- [ ] Audio extraction (local, one-shot) and rolling buffer capture (IPTV/live)
- [ ] `feature/subtitles`: language picker, generation state machine, cue rendering overlay
- [ ] Local-file subtitle caching (Room `SubtitleCache`)
- [ ] Translation step wired in on the VPS

## Phase 4 — 2026 Vision & Polish (The "MX Pro" Level)
- [ ] **AI-Powered "Scene Search"**: Use Whisper transcripts to search for specific moments within a video.
- [ ] **Vertical "Fatafat" Feed**: A dedicated tab for scrolling through short vertical clips found on the device.
- [ ] **Glassmorphism UI**: Dynamic backgrounds that adapt to the color of the current video thumbnail.
- [ ] **Privacy Vault**: A PIN/Biometric-locked section for hiding specific folders and files.
- [ ] **Floating Window Mode**: Fully resizable and draggable pop-up player for extreme multitasking.
- [ ] **FAST Channel Engine**: Integrated linear streaming for "Live TV" without a provider login.
- [ ] **WhatsApp Status Saver**: Automatically scanning and saving statuses.

## Phase 5 — Windows (separate build, later)
- [ ] Decide Windows tech stack independently
- [ ] Port core feature set using the same VPS API

## Deferred
- Chromecast/external display casting
- Multi-profile support
- Cloud sync of favorites/history across devices

## Immediate Next Task
We have completed the **MX Player style file system and advanced playback logic**. The next critical path is **Phase 2: IPTV Integration**, specifically building the M3U parser and the provider login screen.
