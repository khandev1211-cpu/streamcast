# 14 — UI/UX Design System

## Design direction

Dark-first, modern, minimal-chrome-during-playback. The player itself should feel like the star of the screen — controls fade away quickly, typography is clean, and navigation between Library/IPTV/Live/Subtitles/Settings is fast and unambiguous.

## Color

- **Base**: near-black background (not pure `#000000` — a very dark neutral, e.g., `#0E0E12`, reduces harsh contrast and OLED smearing artifacts while still feeling "dark mode").
- **Surface elevation**: slightly lighter dark tones for cards/sheets (channel lists, settings panels) to create depth without relying on shadows, which read poorly on dark backgrounds.
- **Accent color**: a single vibrant accent (e.g., a saturated blue or purple) used sparingly — for the play button, active nav item, and the "Generate Subtitles" call-to-action specifically, since that's the differentiating feature and deserves visual emphasis.
- **Semantic colors**: distinct, consistent colors for error states (stream failed, VPS unreachable) vs. informational states (buffering, syncing) — don't reuse the accent color for both success and error contexts.

## Typography

- A clean, modern sans-serif (system default like Roboto is fine, or a distinct choice like Inter if the app wants slightly more personality).
- Clear hierarchy: large bold titles for screen headers, medium weight for channel/file names, smaller regular weight for metadata (duration, category, "now playing" EPG text).

## Layout patterns

- **Bottom navigation** (Library / IPTV / Live / Settings) — thumb-reachable, standard Android pattern, appropriate for a media app used one-handed often.
- **Grid or list toggle** for local library and channel lists — grids suit visual browsing (thumbnails/logos), lists suit dense channel scanning; let users pick based on preference.
- **Full-screen player** with auto-hiding controls — tap to reveal, auto-hide after a few seconds of inactivity, standard for video apps.

## Subtitle-specific UI

- **"Generate Subtitles" entry point**: a clearly visible button in the player controls (not buried in a menu), given this is the flagship feature.
- **Language picker**: a searchable bottom sheet or dropdown, not a long unfiltered list, given the number of languages Whisper supports.
- **Loading state**: a subtle progress indicator near the subtitle area itself (not a full-screen blocking spinner) so users can keep watching while subtitles generate.
- **Subtitle styling controls**: size, color, background opacity — exposed in Settings and/or a quick-access overlay during playback, since readability varies a lot by content and personal preference.
- **Error state**: a small, dismissible inline message ("Couldn't reach subtitle server — check your connection or VPS status") rather than an intrusive dialog that interrupts playback.

## Motion

- Fast, subtle transitions (150–250ms) between screens — nothing that feels sluggish on a media-first app.
- Controls fade in/out rather than abruptly appearing/disappearing.
- Avoid heavy motion during active playback (e.g., no bouncy animations near the video itself) — motion should support navigation, not distract from content.

## Accessibility

- Sufficient contrast ratios even in dark theme (test accent-on-background combinations, not just assume dark mode is automatically accessible).
- Subtitle text must remain legible over any video content — a semi-transparent background behind subtitle text (not just text with no backdrop) protects readability over bright/busy video frames.
- Touch targets (play/pause, seek, favorite star, etc.) sized per standard Android accessibility guidelines (minimum ~48dp).

## Component reuse across features

- A single `ChannelListItem` composable is shared between IPTV and Live-stream screens (since both are `Channel` entities) — differing only in whether EPG info is shown.
- A single `PlayerControlsOverlay` composable is shared across all playback contexts (local/IPTV/live), with subtitle controls always present but EPG "now/next" info conditionally shown only when available.

See `15-NAVIGATION-SCREENS.md` for the full screen inventory and navigation graph.
