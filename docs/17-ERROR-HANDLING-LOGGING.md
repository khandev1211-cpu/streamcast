# 17 — Error Handling & Logging

## Philosophy

Every failure mode in this app should be **specific and actionable**, not a generic "something went wrong." Given the app depends on a personally-run VPS (not a big, always-on cloud service) and on IPTV sources of varying quality, failures are expected to happen regularly during normal use — the app should handle them as a normal part of the experience, not an edge case.

## Categories of errors

| Category | Examples | Handling approach |
|---|---|---|
| Network connectivity | No internet, DNS failure | Detect upfront, show "no connection" state before attempting requests |
| VPS-specific | Unreachable, slow/timeout, model error, auth failure | Distinguish via `/health` check + specific error codes (see `13-NETWORKING-API-CONTRACTS.md`) |
| IPTV/playlist | Invalid M3U, bad Xtream credentials, dead channel link | Per-source and per-channel error handling — one bad channel shouldn't break the whole list |
| Live stream URL | Unreachable, unsupported format, geo-restricted | Validation at add-time + graceful play-time failure messaging |
| Local file | Unsupported codec, corrupted file, permission denied | Clear per-file error, skip during library scan rather than crashing the scan |
| Storage | Disk full (subtitle cache growing), Room migration failure | Defensive checks before writes; user-visible storage management screen |

## Result wrapper (recap from `13-NETWORKING-API-CONTRACTS.md`)

All repository-layer operations that can fail return a `NetworkResult<T>` (or a similar `Result`-style wrapper for non-network operations like local file scanning), so ViewModels have one consistent pattern to handle regardless of the underlying operation.

## User-facing error messaging principles

- **Be specific where possible**: "Couldn't reach your subtitle server" is more useful than "Error." "This channel's stream link appears broken" is more useful than "Playback failed."
- **Suggest a next step**: a "Retry" button, a link to `VpsConfigScreen` when VPS-related, a suggestion to check the source URL when IPTV-related.
- **Don't block unrelated functionality**: a subtitle generation failure shouldn't prevent the user from continuing to watch the video without subtitles; a single dead IPTV channel shouldn't block the rest of that provider's channel list from displaying.

## Logging strategy

- Use a structured logging approach (e.g., Timber) rather than raw `Log.d` calls scattered through the codebase, so log levels and tags are consistent.
- Log network failures with enough context to debug later (endpoint, error code, timestamp) but **never log sensitive data** — IPTV credentials, VPS API keys, or full stream URLs that might embed credentials should be redacted in logs (see `19-SECURITY-PRIVACY.md`).
- In debug builds, verbose logging is fine; in release builds, keep logging minimal and avoid logging anything that could leak user data if a log file were ever shared (e.g., for bug reports).

## Crash reporting (recommendation)

- Integrate a crash reporting tool (e.g., Firebase Crashlytics or an open-source alternative) before any wider release, so real-world crashes surface with stack traces rather than relying on user bug reports alone. Not required for early development, but worth planning for before Phase 4 polish.

## Graceful degradation examples

- VPS unreachable → video still plays normally, just without the subtitle feature available (clear message, not a broken/frozen player).
- EPG data missing for a channel → channel still plays, "now/next" UI section simply doesn't render rather than showing empty/broken placeholders.
- One IPTV source fails to sync → other configured sources remain usable; only the failing source shows an error state.
