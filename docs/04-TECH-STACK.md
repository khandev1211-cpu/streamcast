# 04 — Tech Stack

## Android application

| Layer | Choice | Rationale |
|---|---|---|
| Language | Kotlin | Standard, modern, first-class Android support |
| UI toolkit | Jetpack Compose | Declarative UI matches the "modern design" goal; faster iteration, easier theming than XML views |
| Playback engine | Media3 (ExoPlayer) | Actively maintained by Google; handles local files, HLS, DASH, and RTMP (via extension); has built-in subtitle rendering hooks |
| Local persistence | Room | Type-safe SQLite wrapper; standard for library/favorites/history/subtitle-cache data |
| Dependency Injection | Hilt | Standard DI for Android; keeps feature modules decoupled and testable |
| Networking | Retrofit + OkHttp | For backend API calls, IPTV playlist/EPG fetches; OkHttp interceptors handle auth headers, logging, retries |
| Async | Kotlin Coroutines + Flow | Standard for async work: network calls, subtitle generation streams, playback state observation |
| Image loading | Coil | Compose-friendly, lightweight, for any channel logos/thumbnails from IPTV metadata |
| JSON parsing | kotlinx.serialization or Moshi | For backend API responses and Xtream Codes API JSON |

## Subtitle backend

| Component | Choice | Rationale |
|---|---|---|
| Model | Whisper Large | High transcription accuracy across many languages |
| Serving | Python + FastAPI (recommended) | Lightweight, async-friendly, easy to wrap a model inference call as a REST endpoint |
| Reverse proxy | nginx | TLS termination, basic auth/API key enforcement |
| Translation (non-English targets) | Separate step after Whisper transcription | Whisper's built-in `translate` task only outputs English; other target languages need a second translation call (see `11-TRANSLATION-STRATEGY.md`) |
| Queueing (optional, if load increases) | Simple job queue (e.g., Redis + RQ/Celery) | Keeps concurrent requests from overwhelming the backend's inference capacity |

## IPTV & live stream parsing

| Concern | Approach |
|---|---|
| M3U/M3U8 playlists | Custom lightweight parser or an existing Kotlin/Java M3U parsing library |
| Xtream Codes API | REST calls following the widely-documented (if unofficial) Xtream Codes spec used across IPTV providers |
| EPG | XMLTV format parsing, decoupled from the playlist parser since not all providers supply it |
| Live URL validation | Media3's own probing (attempt to open/prepare the media source) as a lightweight "is this playable" check |

## Explicitly not chosen (for now, and why)

- **Kotlin Multiplatform (KMP)** — Windows is planned as a fully separate native build in a later phase, so there's no shared-codebase requirement driving KMP adoption right now. Revisit only if Windows work starts soon after Android ships.
- **On-device Whisper (whisper.cpp or similar)** — skipped for MVP since Whisper Large is handled by the backend; could be added later purely as an offline fallback (see `02-FEATURES.md`, deferred features).
- **A big-cloud STT/translation API (Google/Azure/AWS)** — skipped in favor of the custom backend approach; documented here so future contributors understand this was a deliberate choice, not an oversight.

## Versioning note

Specific library versions aren't pinned in this doc since they'll drift over time — check current stable releases of Media3, Compose, Hilt, etc. at actual project-setup time rather than trusting a version number written during the planning phase.
