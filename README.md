# 🎥 StreamCast: The Ultimate Unified Media Experience

[![Android Build](https://github.com/khandev1211-cpu/streamcast/actions/workflows/android.yml/badge.svg)](https://github.com/khandev1211-cpu/streamcast/actions/workflows/android.yml)
[![Version](https://img.shields.io/badge/version-v1.0.0--beta-blue.svg)](https://github.com/khandev1211-cpu/streamcast)
[![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-purple.svg)](https://kotlinlang.org)

**StreamCast** is a pro-grade, unified media player for Android designed with the premium UX of MX Player and the power of modern AI. It seamlessly integrates local media playback, global IPTV, and live streams into a single, high-performance application.

---

## 🌟 Key Features

### 📡 Advanced IPTV & Live Streaming
- **Multi-Source Support**: Full compatibility with M3U playlists, M3U8 links, and Xtream Codes API.
- **Smart Reliability**: Built-in "Auto-Skip" for dead links and background "Health Checks" (Green/Red indicators).
- **Pro Player Profiles**: Specialized User-Agent profiles (VLC, TiviMate, JioTV, Pakistan Zap) to unlock restricted streams.
- **EPG Integration**: Real-time TV Guide with program progress tracking.
- **Global Discovery**: "Browse by Country" system supporting 200+ regions.

### 🎬 Professional Local Player
- **MX Player Style UX**: Intuitive central gesture overlays for Volume, Brightness, and Seeking.
- **High-Performance Engine**: Powered by Media3/ExoPlayer with optimized hardware acceleration.
- **Advanced Controls**: Support for A-B Repeat, Sleep Timer, and precise Resize Modes (Fit, Fill, Zoom, Stretch).

### 🤖 AI-Powered Capabilities (Experimental)
- **AI Subtitles**: On-demand transcription and translation using Whisper Large API.
- **Smart Headers**: Automatic security header injection to bypass geo-blocking.

---

## 🛠️ Tech Stack

- **UI**: Jetpack Compose (Modern, Declarative UI)
- **Engine**: Media3 / ExoPlayer (Industry standard playback)
- **Database**: Room (Offline caching and history)
- **Dependency Injection**: Hilt / Dagger
- **Networking**: OkHttp3 & Retrofit (High-speed stream handling)
- **Asynchronous**: Kotlin Coroutines & Flow

---

## 📂 Project Structure

The project follows a modular Clean Architecture:
- `:app`: Entry point and navigation.
- `:feature:iptv`: Complete IPTV logic, parsing, and folder management.
- `:feature:library`: Local video/audio management and playback.
- `:core:player`: Low-level Media3 implementation and network interceptors.
- `:core:database`: Local storage schema and DAO implementation.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana or newer.
- Android SDK Level 34+.
- Real device running Android 8.0 (Oreo) to Android 14 (U) recommended.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/khandev1211-cpu/streamcast.git
   ```
2. Open in Android Studio.
3. Sync Gradle and run on your device.

---

## 📑 Documentation

Detailed guides are available in the [docs/](docs/) folder:
- [Architecture Deep Dive](docs/03-ARCHITECTURE.md)
- [IPTV Integration Guide](docs/07-IPTV-INTEGRATION.md)
- [MX Player Parity Checklist](docs/25-MX-PLAYER-FEATURE-PARITY.md)

---

## 🤝 Contribution

We welcome contributions! Please feel free to submit Pull Requests or open Issues for stream link compatibility or UI improvements.

---

## 📜 License

StreamCast is developed by **khandev1211-cpu**. All rights reserved.
