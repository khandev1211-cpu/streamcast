# StreamCast

<p align="center">
  <b>Universal Media, IPTV & Live Player with AI Subtitles</b><br>
  <i>A modern Android media player experience.</i>
</p>

---

StreamCast is a feature-rich Android media player designed to unify your content viewing experience. Whether it's local files, IPTV playlists (M3U/Xtream), or custom live streams (HLS/DASH/RTMP), StreamCast handles them all through a single, high-performance pipeline.

Its standout feature is **on-demand AI-generated subtitles**, powered by a self-hosted Whisper Large API, allowing you to generate real-time transcription and translation for any video source.

## 🚀 Key Features

- **Universal Playback**: Unified engine powered by **Media3 (ExoPlayer)** for local files, HLS, DASH, and RTMP.
- **IPTV Integration**: Full support for M3U/M3U8 playlists and Xtream Codes provider logins with EPG (XMLTV) support.
- **AI-Powered Subtitles**: 
  - **One-shot**: Generate and cache subtitles for local media.
  - **Rolling**: Near-real-time subtitles for live streams and IPTV channels.
  - **Multi-language**: Support for transcription and translation into any target language.
- **Live Stream Manager**: Add and save any live HLS/DASH/RTMP URL as a custom channel.
- **Modern UI**: Built entirely with **Jetpack Compose** for a fluid, dark-first design.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Playback Engine**: Media3 / ExoPlayer
- **Database**: Room
- **Networking**: Retrofit & OkHttp
- **Dependency Injection**: Hilt
- **Concurrency**: Coroutines & Flow
- **AI Backend**: Whisper Large (Self-hosted Python/FastAPI)

## 📖 Documentation

The project is backed by comprehensive architectural documentation:

| # | Topic | Description |
|---|---|---|
| 01 | [Overview](docs/01-OVERVIEW.md) | Project scope and guiding principles |
| 03 | [Architecture](docs/03-ARCHITECTURE.md) | MVVM + Repository modular design |
| 05 | [Project Structure](docs/05-PROJECT-STRUCTURE.md) | Module and package layout |
| 09 | [Subtitle Pipeline](docs/09-SUBTITLE-PIPELINE.md) | End-to-end AI generation flow |
| 10 | [VPS Setup](docs/10-VPS-WHISPER-SETUP.md) | Guide for self-hosting the Whisper API |

*See the [Full Documentation Index](README.md#full-documentation-index) for all 24 design documents.*

## 🗺️ Roadmap

- [ ] **Phase 1 (Android MVP)**: Core player, IPTV support, and local library.
- [ ] **Phase 2 (AI Integration)**: Finalizing the Whisper API integration and rolling subtitle engine.
- [ ] **Phase 3 (Windows Support)**: Separate native Windows build.

## 🛠️ Getting Started

*The project is currently in the **architectural setup phase**. Code implementation is ongoing.*

To explore the design, check the `docs/` folder.

---
**Developed by [khandev1211-cpu](https://github.com/khandev1211-cpu)**
