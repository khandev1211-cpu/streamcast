# 13 — Networking & API Contracts

## Overview of network dependencies

The app talks to three kinds of external endpoints:

1. **The self-hosted VPS** — Whisper subtitle generation (`/transcribe`, `/health`).
2. **IPTV sources** — M3U playlist URLs (plain text fetch) and Xtream Codes APIs (JSON REST).
3. **EPG sources** — XMLTV data, either bundled with the Xtream API or a separate URL.

## VPS API contract (subtitle generation)

### `POST /transcribe`

**Request** (multipart form or base64 JSON — pick one consistently):
```json
{
  "audio": "<base64-encoded audio chunk>",
  "source_language": "auto",
  "target_language": "es",
  "timestamps": true,
  "mode": "one_shot" 
}
```
- `mode`: `"one_shot"` for local files (full track), `"rolling"` for IPTV/live (chunked). This lets the VPS apply different chunking/context handling if useful, even though the endpoint shape is otherwise identical.

**Response — success (200)**:
```json
{
  "segments": [
    { "start": 0.0, "end": 3.2, "text": "Hello and welcome." },
    { "start": 3.2, "end": 6.8, "text": "Today we are discussing..." }
  ],
  "detected_language": "en"
}
```

**Response — error (4xx/5xx)**:
```json
{ "error": "model_overloaded", "message": "Server is busy, try again shortly." }
```

Recommended error codes: `invalid_audio`, `unsupported_language`, `model_overloaded`, `internal_error`, `unauthorized`.

### `GET /health`
```json
{ "status": "ok", "model_loaded": true }
```
Used by the app to distinguish "VPS unreachable" from "VPS reachable but this request failed" — see `09-SUBTITLE-PIPELINE.md`.

## Client-side networking layer

- **Retrofit** service interfaces define these calls; a dedicated `VpsApiClient` in `core/network` wraps them.
- **OkHttp interceptor** attaches the `Authorization: Bearer <api-key>` header to every VPS request (the key itself lives in secure app storage — see `19-SECURITY-PRIVACY.md`).
- **Timeouts**: VPS inference can be slow, especially on modest hardware — use a longer read timeout for `/transcribe` than for typical REST calls (e.g., 30–60s) rather than the default short timeout, to avoid false "failure" states on requests that are simply still processing.

## IPTV networking

- **M3U fetch**: a plain HTTP GET returning text; parsed client-side (see `07-IPTV-INTEGRATION.md`).
- **Xtream Codes API**: REST/JSON, credentials passed as query params per the (unofficial) spec providers use. Responses vary somewhat between providers in practice — parsing should tolerate missing/extra fields rather than failing strictly on any schema mismatch.
- **EPG (XMLTV)**: fetched as XML, parsed with a streaming XML parser if files are large, to avoid loading an entire multi-day schedule into memory at once.

## Result wrapper pattern

All network calls return a consistent wrapper so the UI layer handles success/error/loading uniformly:

```kotlin
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T): NetworkResult<T>()
    data class Error(val code: String, val message: String): NetworkResult<Nothing>()
    object Loading: NetworkResult<Nothing>()
}
```

This applies equally to VPS calls, IPTV fetches, and EPG fetches — one pattern, not three separate ad-hoc error-handling approaches.

## Retry & backoff

- **VPS `/transcribe`**: a single retry on timeout is reasonable; repeated automatic retries risk compounding load on a single personal VPS — better to surface a clear "try again" action to the user after one automatic attempt.
- **IPTV/EPG fetches**: standard exponential backoff for transient network failures is fine, since these are typically larger third-party infrastructure that can handle it.

## Offline/no-connectivity behavior

- Local file playback and previously cached subtitles work fully offline.
- IPTV, live streams, and new subtitle generation all require connectivity — the app should detect lack of connectivity upfront (rather than letting requests fail with a generic timeout) and show a clear "no internet connection" state.
