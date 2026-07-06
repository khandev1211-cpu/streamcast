# 24 — Glossary

**Media3 / ExoPlayer** — Google's modern Android media playback library, successor branding to the standalone ExoPlayer project; handles local file, HLS, DASH playback natively, with RTMP support via an extension.

**M3U / M3U8** — Plain-text playlist file formats listing media/stream entries; M3U8 specifically refers to UTF-8 encoded M3U, commonly used for HLS playlists and IPTV channel lists.

**HLS (HTTP Live Streaming)** — Apple-originated streaming protocol using `.m3u8` manifest files pointing to segmented media; widely used for both IPTV and general live streaming.

**DASH (Dynamic Adaptive Streaming over HTTP)** — An alternative adaptive streaming protocol, using `.mpd` manifest files; conceptually similar goals to HLS.

**RTMP (Real-Time Messaging Protocol)** — An older streaming protocol, still used by some live stream sources; requires an ExoPlayer extension since it's not in Media3 core.

**Xtream Codes API** — A widely-used (though unofficial/community-documented) REST/JSON API pattern that many IPTV providers implement, authenticated via host/username/password, exposing channel lists, categories, and EPG data.

**EPG (Electronic Program Guide)** — Schedule/program metadata for TV channels (what's airing now, what's next), typically delivered in XMLTV format.

**XMLTV** — An XML-based format standard for representing TV programming schedules (EPG data).

**Whisper / Whisper Large** — OpenAI's open-weight speech-to-text model family; "Large" refers to the biggest, most accurate variant, at the cost of higher compute requirements.

**faster-whisper** — A reimplementation of Whisper inference using CTranslate2, offering significantly faster inference than the original `openai-whisper` package on the same hardware, for the same model weights.

**Transcription vs. Translation (Whisper-specific)** — Whisper's `transcribe` task outputs text in the spoken (source) language; its `translate` task outputs English only, regardless of source language — a key limitation driving this project's separate translation step for other target languages.

**MediaSource (this project's internal model)** — The unifying data model representing any playable content (local file, IPTV channel, or user-added live URL) with a common shape (`uri`, `type`, `isCacheable`, optional metadata) so the player, UI, and subtitle systems don't need source-specific branching wherever avoidable.

**SubtitleCache** — This project's Room entity storing previously generated subtitle cues for local files, keyed by media ID and language, to avoid regenerating subtitles on repeat playback.

**Rolling / one-shot subtitle modes** — This project's terminology for the two subtitle generation flows: one-shot (a full local file sent once) versus rolling (a continuously-updating buffer sent in chunks for IPTV/live content).

**VPS (Virtual Private Server)** — A rented virtual server (as opposed to a big managed cloud AI service) where the developer self-hosts the Whisper-based subtitle inference API for this project.

**Kotlin Multiplatform (KMP)** — A Kotlin feature allowing shared business logic across platforms (e.g., Android and desktop/JVM); explicitly not adopted in this project's current plan, since Windows is a separate future build (see `23-WINDOWS-FUTURE-PLAN.md`).

**Hilt** — Google's recommended dependency injection framework for Android, built on top of Dagger.

**Room** — Android's official SQLite abstraction/ORM library, used here for local library, favorites, history, and subtitle cache persistence.

**Jetpack Compose** — Android's modern declarative UI toolkit, used throughout this project's UI layer in place of the older XML View system.
