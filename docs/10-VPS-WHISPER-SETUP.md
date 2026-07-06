# 10 — VPS Whisper API Setup

This doc covers the backend the developer is setting up themselves: Whisper Large running in Python on a personal VPS, exposed as an API the Android app calls.

## Recommended stack

- **Python 3.10+**
- **FastAPI** — async-friendly, easy to structure as a REST API, good fit for wrapping a model inference call.
- **Whisper Large** — via the `openai-whisper` package, or a faster inference implementation (e.g., `faster-whisper`, which uses CTranslate2 and is significantly quicker on CPU/GPU for the same model weights) — worth evaluating `faster-whisper` specifically since VPS inference speed directly affects subtitle latency, especially for live streams.
- **nginx** as a reverse proxy in front of the FastAPI app, handling TLS and basic access control.
- **systemd** (or a process manager like `supervisor`) to keep the API service running and auto-restart on crash/reboot.

## Minimal endpoint contract

```
POST /transcribe
Headers:
  Authorization: Bearer <api-key>
Body (multipart or base64):
  audio: <audio bytes>
  source_language: "auto" | "en" | "ur" | ... 
  target_language: "en" | "ur" | "es" | ...
  timestamps: true

Response 200:
{
  "segments": [
    { "start": 0.0, "end": 3.2, "text": "..." },
    { "start": 3.2, "end": 6.8, "text": "..." }
  ]
}

Response 4xx/5xx:
{ "error": "description of what went wrong" }
```

See `13-NETWORKING-API-CONTRACTS.md` for the full contract including error codes.

## Hardware considerations

- Whisper Large is the most accurate but also the heaviest Whisper variant — CPU-only inference can be slow (potentially many seconds per chunk depending on VPS specs). If the VPS has no GPU, this directly affects how "near real-time" live subtitles can actually feel.
- If GPU isn't available, consider whether Whisper **medium** offers an acceptable accuracy/speed trade-off for live-stream subtitles specifically, while reserving **large** for one-shot local-file transcription where latency matters less. This is a judgment call based on actual VPS hardware — worth benchmarking early rather than assuming.

## Security basics

- **Do not expose the endpoint without authentication** — even though this is a personal project, an open inference endpoint on a public IP can be found and abused (unwanted load, cost if you're on metered bandwidth/compute). A simple API key checked via an `Authorization` header, enforced at the nginx layer or in FastAPI middleware, is sufficient for a single-app use case.
- **Rate limiting** at the nginx level (e.g., `limit_req`) protects against runaway client bugs (like an infinite retry loop) hammering the VPS.
- **HTTPS only** — since audio content (potentially personal/private video audio) is being transmitted, use a TLS certificate (e.g., via Let's Encrypt/Certbot) rather than plain HTTP.

## Handling concurrent requests

- A single Whisper Large inference call can be resource-intensive; if the app ever sends overlapping requests (e.g., regenerating subtitles while a previous chunk is still processing), the VPS could get overloaded.
- A simple in-process queue (or a lightweight task queue like Redis + RQ) ensures requests are processed one at a time rather than the server thrashing under concurrent model inference calls, if this becomes an issue.

## Deployment checklist

- [ ] Whisper Large (or faster-whisper) installed and benchmarked on the actual VPS hardware for a realistic per-chunk latency estimate.
- [ ] FastAPI app wrapping the model, exposing `/transcribe`.
- [ ] Translation step wired in for non-English targets (see `11-TRANSLATION-STRATEGY.md`).
- [ ] nginx reverse proxy with TLS and API-key enforcement.
- [ ] systemd service (or equivalent) so the API survives VPS reboots and crashes.
- [ ] Basic logging (request received, processing time, errors) to debug issues without guessing.
- [ ] A `/health` endpoint for the app to check VPS reachability without doing a full transcription request.

## Suggested `/health` endpoint

```
GET /health
Response: { "status": "ok", "model_loaded": true }
```

The Android app can ping this on startup or before attempting subtitle generation, to distinguish "VPS is down" from "VPS is up but this specific request failed" in its error messaging.
