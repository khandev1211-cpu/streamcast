# 05 — Project Structure

## Repository layout (Android app, Phase 1)

```
streamcast-android/
├── app/                          # Application module (entry point, DI setup)
│   └── src/main/
│       ├── java/com/streamcast/app/
│       │   ├── StreamCastApp.kt      # Application class, Hilt entry
│       │   └── MainActivity.kt        # Single-activity host for Compose nav
│       └── AndroidManifest.xml
│
├── core/
│   ├── player/                   # ExoPlayer/Media3 wrapper module
│   │   └── src/main/java/com/streamcast/core/player/
│   │       ├── PlayerManager.kt
│   │       ├── PlaybackState.kt
│   │       └── MediaSource.kt
│   ├── network/                  # Shared networking module
│   │   └── src/main/java/com/streamcast/core/network/
│   │       ├── VpsApiClient.kt
│   │       ├── IptvApiClient.kt
│   │       └── NetworkResult.kt
│   └── database/                 # Room module
│       └── src/main/java/com/streamcast/core/database/
│           ├── AppDatabase.kt
│           ├── entities/
│           └── dao/
│
├── feature/
│   ├── library/
│   │   └── src/main/java/com/streamcast/feature/library/
│   │       ├── ui/                # Compose screens
│   │       ├── viewmodel/
│   │       └── LibraryRepository.kt
│   ├── iptv/
│   │   └── src/main/java/com/streamcast/feature/iptv/
│   │       ├── ui/
│   │       ├── viewmodel/
│   │       ├── parser/            # M3U/Xtream/EPG parsing
│   │       └── IptvRepository.kt
│   ├── live/
│   │   └── src/main/java/com/streamcast/feature/live/
│   │       ├── ui/
│   │       ├── viewmodel/
│   │       └── LiveStreamRepository.kt
│   └── subtitles/
│       └── src/main/java/com/streamcast/feature/subtitles/
│           ├── ui/
│           ├── viewmodel/
│           ├── SubtitleRepository.kt
│           └── SubtitleRenderer.kt
│
├── ui/
│   └── theme/
│       └── src/main/java/com/streamcast/ui/theme/
│           ├── Color.kt
│           ├── Typography.kt
│           └── Theme.kt
│
├── settings/
│   └── src/main/java/com/streamcast/settings/
│       ├── ui/
│       └── SettingsRepository.kt
│
├── build.gradle.kts               # Root build file
├── settings.gradle.kts            # Module registration
└── gradle.properties
```

## Naming conventions

- **Modules**: lowercase, hyphen-free, matching Gradle module names (`:core:player`, `:feature:iptv`).
- **Packages**: `com.streamcast.<layer>.<module>` mirroring the folder structure.
- **ViewModels**: `<Feature>ViewModel.kt` (e.g., `LibraryViewModel.kt`).
- **Repositories**: `<Feature>Repository.kt`, interface + impl split if DI benefits from it (e.g., `SubtitleRepository` interface, `SubtitleRepositoryImpl` for the real network-backed version, useful for testing with fakes).

## Why multi-module (not a single `app` module)

- **Build performance** — Gradle can build/cache unrelated modules in parallel; changing subtitle UI doesn't force a full rebuild of IPTV parsing code.
- **Enforced boundaries** — a feature module can't accidentally reach into another feature's internals; everything shared goes through `core/*`, keeping the `MediaSource` abstraction actually enforced rather than just a convention.
- **Testability** — each module can have its own test source set, and repositories can be faked per-module without spinning up the whole app.

## Non-Android directories (repo root, if using a monorepo-style layout)

```
streamcast/
├── android/           # this project
├── vps-subtitle-api/  # Python/FastAPI Whisper service (see 10-VPS-WHISPER-SETUP.md)
└── docs/              # this documentation set
```

Keeping the VPS API code in the same repo (even though it deploys separately) makes it easier to keep the API contract between app and backend in sync — see `13-NETWORKING-API-CONTRACTS.md`.
