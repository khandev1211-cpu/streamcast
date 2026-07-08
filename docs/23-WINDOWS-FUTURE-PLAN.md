# 23 — Windows Future Plan

## Scope of this doc

High-level thinking for the eventual Windows version, to be built as a **separate native application** after the Android app has shipped — not a shared codebase, per the project's stated direction.

## Why separate, not shared, for now

- Avoids taking on Kotlin Multiplatform (or a cross-platform framework) complexity before the Android app itself is proven out and stable.
- Lets the Windows app be built with whatever's the best-fit native stack at that future time, rather than constraining today's Android architecture decisions around hypothetical cross-platform reuse.
- The one genuinely shared piece across both platforms is the **subtitle backend API** — since it's a platform-agnostic HTTP service, both the Android app and a future Windows app can call the same backend without any client-side code sharing at all.

## What can realistically carry over conceptually (not as code)

- The **feature scope** (local playback, IPTV, live streams, AI subtitles) — same product, same core value proposition.
- The **`MediaSource`/`Channel` conceptual model** — even in an entirely different codebase, keeping this same mental model (a unified source abstraction, subtitle generation as an on-demand action) keeps the two apps consistent in behavior and easier for one developer to reason about across both.
- The **backend API contract** (`13-NETWORKING-API-CONTRACTS.md`) — this doesn't change based on client platform, so Windows development can reuse the exact same `/transcribe` and `/health` contract, potentially even reusing recorded API test cases from Android development.

## Candidate Windows tech stacks (to evaluate when that phase actually starts)

| Option | Notes |
|---|---|
| **WPF (.NET)** | Mature, well-documented, good media playback library support (e.g., via LibVLCSharp for broad format support similar to what Media3 provides on Android) |
| **WinUI 3 / .NET MAUI** | More modern Windows-native UI framework; MAUI specifically has some cross-platform ambitions but would still be a separate build from the Android Kotlin app |
| **Electron / web-based** | Faster to build a UI, but media playback performance and native feel are typically weaker than a true native app — likely not the best fit for a media-playback-focused app |

No decision is made here — this is intentionally left open until Windows development actually starts, since tooling and best practices will likely shift by then anyway.

## Things to plan for, even during Android development, to make Windows easier later

- Keep the backend API contract clean and documented (already covered in `13-NETWORKING-API-CONTRACTS.md`) so it's trivial to integrate a second, independent client against it.
- Keep IPTV parsing logic (M3U/Xtream/EPG format knowledge) documented as *domain knowledge* (see `07-IPTV-INTEGRATION.md`) even though the Android implementation itself won't be reused — the format knowledge and edge cases discovered during Android development are valuable reference material for a Windows parser built independently later.
- Avoid baking client-specific assumptions into the backend itself — e.g., don't design the `/transcribe` request format around anything Android-specific; keep it a generic HTTP/JSON contract any client can call.

## Explicitly not a goal right now

- Building any shared library, shared module, or cross-platform abstraction ahead of actual Windows development starting. Speculative reuse infrastructure built before it's needed tends to add complexity without paying off, especially given how far off the Windows phase currently is.
