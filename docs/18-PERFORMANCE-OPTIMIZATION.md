# 18 — Performance & Optimization

## Areas that matter most for this app

Given the feature set, performance work should focus on: playback smoothness across variable-quality network sources, subtitle generation latency, battery/data usage during live-stream subtitle generation, and local library scan speed on devices with large media collections.

## Playback performance

- Use Media3's built-in adaptive buffering configuration rather than fighting it with custom buffering logic — tune buffer duration settings only after observing real stalling/rebuffering issues, not preemptively.
- For IPTV/live sources specifically, expect more variable network conditions than local files — surface buffering state clearly (see `06-PLAYBACK-ENGINE.md`) rather than letting the UI look frozen during a rebuffer.
- Avoid re-creating the `ExoPlayer` instance on every navigation; `PlayerManager` should reuse a single instance and swap `MediaItem`s, which is significantly cheaper than tearing down and rebuilding the player.

## Local library scanning

- Scanning device storage for media files should run on a background dispatcher and be incremental/observable (e.g., emit files as they're found via `Flow`) rather than blocking the UI until the entire scan completes — especially important for users with large local libraries.
- Cache scan results in Room and only re-scan on explicit refresh or detected storage changes (e.g., via `MediaStore` content observer), rather than rescanning the whole device every time the Library screen opens.

## Subtitle generation performance

- **Latency is dominated by VPS inference time**, not client-side work — the biggest lever here is on the VPS side (see `10-VPS-WHISPER-SETUP.md`: model choice, GPU vs CPU, `faster-whisper` vs standard `openai-whisper`).
- Client-side, keep audio chunk sizes for live/rolling mode reasonable (not so small that per-request overhead dominates, not so large that latency balloons) — this needs empirical tuning against actual VPS response times once the backend is running.
- Avoid holding decoded audio buffers longer than necessary — for rolling-mode capture, discard old buffer data once it's been sent, rather than accumulating an ever-growing in-memory buffer.

## Battery & data usage

- Continuous live-stream subtitle generation (repeated audio chunk uploads) has real battery and mobile-data cost — consider:
  - Only enabling subtitle generation on-demand (already the design — not automatic for every video), which naturally limits this.
  - A "Wi-Fi only for subtitle generation" setting, for users conscious of mobile data usage, given audio chunks are being uploaded repeatedly during rolling mode.
  - Stopping the rolling subtitle loop immediately when the player is paused or backgrounded, not just when subtitles are explicitly toggled off.

## Compose-specific performance

- Avoid unnecessary recompositions in the player controls overlay — since playback position updates frequently (e.g., every second for the seek bar), make sure only the specific composables that need position (progress bar, time label) recompose on each tick, not the entire controls overlay or subtitle renderer.
- Use `derivedStateOf` or scoped state hoisting where a frequently-changing value (like playback position) would otherwise trigger broader recomposition than needed.

## Memory considerations

- Channel logo images (Coil-loaded) across potentially hundreds of IPTV channels should use appropriate caching/downsampling rather than loading full-resolution images for small list-item icons.
- Subtitle cue lists for long local videos (feature-length films) are a modest amount of text data — not a significant memory concern, but avoid holding multiple full cue lists in memory simultaneously if a user switches between cached subtitle languages for the same file.

## Benchmarking recommendation

- Before committing to specific buffer sizes, chunk intervals, or model choices (Whisper Large vs faster-whisper vs a smaller model for live mode), do empirical testing against the actual VPS hardware rather than guessing — these numbers vary enormously based on real infrastructure, and decisions made on assumption here are likely to need revisiting anyway.
