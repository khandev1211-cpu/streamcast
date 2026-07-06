# 22 — Roadmap

## Phase 0 — Foundation
- [x] Define features, architecture, tech stack, and full documentation set
- [x] Set up Android project skeleton (multi-module structure)

## Phase 1 — Core Playback (MX Player Style)
- [x] `core/player` module: `PlayerManager` wrapping Media3/ExoPlayer
- [x] `feature/library`: local file scanning, folder/subfolder navigation
- [x] Advanced Player UI: Gestures (Vol/Bri/Seek), Pinch-to-zoom, Decoder toggle, Speed control
- [x] Base Compose navigation (Bottom nav)
- [x] Room DB schema v1

## Phase 2 — MX Player Refinement (The "Pro" Level)
- [ ] **Background Audio Playback**: Keep playing when app is minimized.
- [ ] **Audio/Subtitle Selector**: Choose between multiple tracks and styles.
- [ ] **A-B Repeat & Sleep Timer**: Looping and auto-stop features.
- [ ] **Library Pro Features**: Advanced sorting, "NEW" tags, and folder hiding.
- [ ] **Global Search**: Search filenames across all device folders.
- [ ] **Kids Lock+**: Interactive touch animations during lock mode.

## Phase 3 — IPTV & Live Streams
- [ ] M3U/M3U8 parser (with malformed-entry tolerance)
- [ ] Xtream Codes API client
- [ ] EPG/XMLTV parsing (optional metadata)
- [ ] `feature/iptv` UI: source management, channel list, favorites, search

## Phase 4 — AI Subtitles (Flagship Feature)
- [ ] `core/network` VPS client (`VpsApiClient`, result wrapper)
- [ ] Audio extraction (local, one-shot) and rolling buffer capture (IPTV/live)
- [ ] `feature/subtitles`: language picker, generation state machine, cue rendering overlay

## Phase 5 — 2026 Vision
- [ ] **Vertical "Fatafat" Feed**: TikTok-style scroll for local clips.
- [ ] **Glassmorphism UI**: Dynamic backgrounds matching video colors.
- [ ] **Privacy Vault**: PIN-locked secure folder.

## Immediate Next Task
The next step is to implement the **MX Player Refinement** items from Phase 2, starting with **Background Audio Playback** and the **Track Selection Menu**.
