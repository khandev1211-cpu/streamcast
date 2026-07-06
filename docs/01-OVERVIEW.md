# 01 — Project Overview

## What this project is

**StreamCast** (working name) is an Android media player that unifies three kinds of playback into one modern app:

1. **Local media** — video and audio files stored on the device.
2. **IPTV** — playlists (M3U/M3U8) and Xtream Codes provider logins, with EPG support.
3. **Live streams** — IPTV channels plus any user-pasted HLS/DASH/RTMP URL.

Its standout feature is **on-demand AI subtitle generation in any language**, for any video regardless of source, powered by a Whisper Large model self-hosted by the developer on a personal VPS.

A Windows version is planned for a later phase, built as a **separate native app** — this doc set does not assume shared code between Android and Windows.

## Who this is for

- Someone who currently juggles multiple apps (a file player, an IPTV app, a browser tab for random live links) and wants one app.
- Non-native speakers or people watching foreign-language content who currently have no subtitle option at all.
- Anyone who wants subtitles for content that will never ship with official ones (home videos, niche live streams, obscure IPTV channels).

## Guiding principles for this build

- **Source-agnostic core** — the player, the UI, and the subtitle system should not care whether a video came from a local file, an IPTV channel, or a pasted URL. They all become a `MediaSource` with a URI once they enter the app.
- **On-demand, not automatic** — subtitle generation is user-triggered, not run for every video by default. This keeps VPS load, latency, and cost predictable.
- **Personal-VPS-aware design** — because the Whisper backend is self-hosted rather than a big-cloud API, the app needs to be resilient to slower response times, occasional downtime, and rate limits that a single VPS naturally has compared to Google/AWS-scale infrastructure.
- **Android first, real product** — this isn't a proof of concept; the docs assume production concerns (error handling, caching, offline behavior) from day one, even though Windows support comes later.

## What this doc set covers

This is a full technical documentation set — 20+ documents — covering product scope, architecture, every major subsystem (playback, IPTV, live streams, subtitles), data design, UI/UX, and the operational concerns (testing, security, performance, CI/CD) needed to actually ship this. See the main `README.md` for the full index.

## What this doc set is not

- Not a line-by-line implementation guide or full source code — it's the architecture and design layer that precedes writing code.
- Not a business/monetization plan — features here are described from a technical/product standpoint, not a pricing or go-to-market one.
