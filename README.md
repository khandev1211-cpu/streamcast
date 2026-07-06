# StreamCast (working name) — Universal Media, IPTV & Live Player with AI Subtitles

A modern Android app unifying local video/audio playback, IPTV (M3U/Xtream), and user-added live streams — with its standout feature: **on-demand AI-generated subtitles in any language**, powered by a self-hosted Whisper Large model on the developer's own VPS.

> Windows support is planned for a later phase as a fully separate native build — no shared codebase with Android in the current plan.

## Quick Summary

- **Playback engine**: Media3/ExoPlayer, handling local files, HLS, DASH, and RTMP through one unified pipeline.
- **IPTV**: M3U/M3U8 playlists and Xtream Codes provider logins, with optional EPG.
- **Live streams**: any user-pasted HLS/DASH/RTMP URL, saved as a channel alongside IPTV ones.
- **AI Subtitles**: transcription + translation into any target language via a self-hosted Whisper Large API, in both one-shot (local files, cached) and rolling (IPTV/live, near-real-time) modes.
- **Stack**: Kotlin, Jetpack Compose, Media3, Room, Hilt, Retrofit, Coroutines/Flow — see full rationale in the docs.

## Full Documentation Index

| # | Document | Covers |
|---|---|---|
| 01 | [OVERVIEW](docs/01-OVERVIEW.md) | What this project is, guiding principles, scope |
| 02 | [FEATURES](docs/02-FEATURES.md) | Full feature spec — MVP, later phases, out of scope |
| 03 | [ARCHITECTURE](docs/03-ARCHITECTURE.md) | System architecture, modules, data flow |
| 04 | [TECH STACK](docs/04-TECH-STACK.md) | Every library/tool choice and why |
| 05 | [PROJECT STRUCTURE](docs/05-PROJECT-STRUCTURE.md) | Repo/module/package layout |
| 06 | [PLAYBACK ENGINE](docs/06-PLAYBACK-ENGINE.md) | Media3/ExoPlayer integration deep dive |
| 07 | [IPTV INTEGRATION](docs/07-IPTV-INTEGRATION.md) | M3U, Xtream Codes, EPG parsing |
| 08 | [LIVE STREAMS](docs/08-LIVE-STREAMS.md) | User-added HLS/DASH/RTMP URL handling |
| 09 | [SUBTITLE PIPELINE](docs/09-SUBTITLE-PIPELINE.md) | End-to-end AI subtitle generation flow |
| 10 | [VPS WHISPER SETUP](docs/10-VPS-WHISPER-SETUP.md) | Self-hosted Whisper Large API setup guide |
| 11 | [TRANSLATION STRATEGY](docs/11-TRANSLATION-STRATEGY.md) | Handling non-English subtitle targets |
| 12 | [DATABASE SCHEMA](docs/12-DATABASE-SCHEMA.md) | Room entities, DAOs, relationships |
| 13 | [NETWORKING & API CONTRACTS](docs/13-NETWORKING-API-CONTRACTS.md) | VPS API contract, IPTV/EPG networking |
| 14 | [UI/UX DESIGN SYSTEM](docs/14-UI-UX-DESIGN-SYSTEM.md) | Visual design, color, typography, motion |
| 15 | [NAVIGATION & SCREENS](docs/15-NAVIGATION-SCREENS.md) | Full screen inventory, nav graph |
| 16 | [STATE MANAGEMENT](docs/16-STATE-MANAGEMENT.md) | ViewModel/state patterns, subtitle state machine |
| 17 | [ERROR HANDLING & LOGGING](docs/17-ERROR-HANDLING-LOGGING.md) | Error categories, messaging, logging strategy |
| 18 | [PERFORMANCE OPTIMIZATION](docs/18-PERFORMANCE-OPTIMIZATION.md) | Playback, subtitle latency, battery/data, Compose perf |
| 19 | [SECURITY & PRIVACY](docs/19-SECURITY-PRIVACY.md) | Credential storage, TLS, permissions, privacy |
| 20 | [TESTING STRATEGY](docs/20-TESTING-STRATEGY.md) | Unit/integration/UI/manual testing approach |
| 21 | [CI/CD & BUILD](docs/21-CI-CD-BUILD.md) | Build variants, pipeline, release process |
| 22 | [ROADMAP](docs/22-ROADMAP.md) | Phased build plan, Android first then Windows |
| 23 | [WINDOWS FUTURE PLAN](docs/23-WINDOWS-FUTURE-PLAN.md) | Later-phase Windows app thinking |
| 24 | [GLOSSARY](docs/24-GLOSSARY.md) | Terms and acronyms used throughout |

## Platform Plan

- **Phase 1 (now → MVP):** Android — Kotlin, Jetpack Compose, Media3
- **Phase 2 (later):** Windows — separate native app, stack decided closer to that phase

## Status

📋 Planning / architecture stage — this documentation set is the foundation before development starts. No application code has been written yet.
