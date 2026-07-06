# 11 — Translation Strategy

## The core problem

Whisper has two native tasks:
- **transcribe** — output text in the same language as the spoken audio.
- **translate** — output text in **English only**, regardless of input language.

This means Whisper alone cannot directly produce, say, Urdu subtitles for an English video, or French subtitles for a Spanish video. A second translation step is required whenever the target language isn't English and doesn't match the source language.

## Recommended pipeline

```
Audio
  │
  ▼
Whisper Large: transcribe (source language, native text + timestamps)
  │
  ▼
If target_language == source_language:
    → done, return transcription as-is
Else:
    → pass transcribed text segments through a translation step
    → return translated text, same timestamps
```

## Translation step options

| Option | Trade-off |
|---|---|
| A local translation model (e.g., an open NLLB or M2M100 variant) run on the same VPS | Keeps everything self-hosted and free of external API costs, but adds VPS load and complexity on top of Whisper |
| A third-party translation API (e.g., a cloud translation service) | Simpler to integrate, likely faster/more accurate for many language pairs, but reintroduces external dependency + potential per-request cost — worth weighing against the "self-hosted" goal that motivated the VPS-Whisper approach in the first place |
| Whisper's own `translate` task, but only for English targets | Zero extra work when the target language happens to be English — use this as a fast path even if a general translation step is also implemented for other languages |

Given the project's stated preference for self-hosting, starting with a local open translation model is the more consistent choice — but it's worth prototyping both to compare actual latency and translation quality before committing.

## Timestamp preservation

- Translation should operate per-segment (using Whisper's own segment boundaries) rather than translating the entire transcript as one block and trying to re-align timestamps afterward — this keeps subtitle timing accurate without extra alignment logic.

## Language coverage caveat

- Whichever translation approach is chosen, its supported-language list may not perfectly match Whisper's transcription language list. The app's language picker should reflect the actual **intersection** of what both stages support, not just what Whisper alone can transcribe, so users aren't offered target languages that will silently fail or fall back incorrectly.

## Testing recommendation

- Before wiring this into the app, test the full transcribe → translate chain manually against a handful of known audio clips in different source languages, checking both translation accuracy and end-to-end latency, since this directly determines how usable live-stream subtitles feel in practice.
