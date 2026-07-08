# 14 — UI/UX Design System

## Design direction

**Explicit reference point: MX Player.** Layout, navigation, and player-screen interaction patterns follow MX Player's proven conventions rather than inventing a new interaction model — this app adds AI subtitles and IPTV/live sources on top of a layout users already know how to use.

Dark-first, modern, minimal-chrome-during-playback. The player itself should feel like the star of the screen — controls fade away quickly, typography is clean, and navigation between Library/IPTV/Live/Subtitles/Settings is fast and unambiguous.

### What "MX Player style" means concretely here

- **Bottom navigation bar** — primary navigation (Video / Audio / IPTV / Profile) sits at the bottom of the screen for easy thumb access, while keeping the player screen full-screen and immersive.
- **Library split by media type** — local media is split into dedicated "Video" and "Audio" sections in the bottom nav, making it easier for users to jump directly to what they want to play.
- **Profile/Settings integrated** — a dedicated "Profile" or "Settings" tab in the bottom nav for quick access to backend configuration and app preferences.
- **Gesture-driven player screen** — 
  - **Left Half (Vertical Swipe):** Screen **Brightness**.
  - **Right Half (Vertical Swipe):** **Volume**.
  - **Horizontal Swipe:** **Seeking** (moving forward or backward).
  - **Pinch-to-Zoom:** Zoom in or out of the video frame.
  - **Double Tap (Center):** Play or Pause the video.
  - **Double Tap (Sides):** Seek ±10s.
  - **Long Press:** Temporary 2x speed boost.
  - **Two-finger Vertical Swipe:** Adjust **Playback Speed** (Stage 4).
- **Control Bar Organization**:
  - **Top Bar**: Back button, Title, Track Selection (Audio/Subtitle), Decoder Type (HW/SW), and Menu (Three-dot).
  - **Bottom Bar**: 
    - **Seekbar**: Full width, showing elapsed time (left) and total/remaining time (right). Tapping elapsed time toggles "Time Remaining" mode.
    - **Playback Row**: Center-aligned icons for *Previous, Rewind (10s), Play/Pause, Fast Forward (10s), and Next*.
    - **Corners**: Lock icon (Bottom Left), Resize/Aspect Ratio icon (Bottom Right).

## Color

- **Base**: near-black background (not pure `#000000` — a very dark neutral, e.g., `#0E0E12`, reduces harsh contrast and OLED smearing artifacts while still feeling "dark mode").
- **Surface elevation**: slightly lighter dark tones for cards/sheets (channel lists, settings panels) to create depth without relying on shadows, which read poorly on dark backgrounds.
- **Accent color**: a single vibrant accent (e.g., a saturated blue or purple) used sparingly — for the play button, active nav item, and the "Generate Subtitles" call-to-action specifically, since that's the differentiating feature and deserves visual emphasis.
- **Semantic colors**: distinct, consistent colors for error states (stream failed, backend unreachable) vs. informational states (buffering, syncing) — don't reuse the accent color for both success and error contexts.

## Typography

- A clean, modern sans-serif (system default like Roboto is fine, or a distinct choice like Inter if the app wants slightly more personality).
- Clear hierarchy: large bold titles for screen headers, medium weight for channel/file names, smaller regular weight for metadata (duration, category, "now playing" EPG text).

## Layout patterns

- **Bottom navigation bar** (Video / Audio / IPTV / Profile) as the primary navigation, ensuring the most common destinations are always one tap away.
- **Grid-first, folder-grouped libraries** — thumbnails in a grid for Video and list/grid for Audio, grouped by folder by default; a toggle switches to a flat compact list for users who prefer scanning by filename.
- **Channel lists (IPTV/Live)** default to a list view (denser, more scannable for potentially hundreds of channels) with channel logos as small leading icons, category headers, and a grid toggle available for users who prefer browsing by logo.
- **Full-screen player** with auto-hiding controls and MX-Player-style gesture zones (see above) — tap to reveal controls, auto-hide after a few seconds of inactivity.
- **Corner "more options" menu** on the player screen — a single icon opening a sheet/panel with subtitle language, subtitle styling, audio track, playback speed, and (for IPTV) EPG details, keeping the main overlay uncluttered.

## Subtitle-specific UI

- **"Generate Subtitles" entry point**: a clearly visible button in the player controls (not buried in a menu), given this is the flagship feature.
- **Language picker**: a searchable bottom sheet or dropdown, not a long unfiltered list, given the number of languages Whisper supports.
- **Loading state**: a subtle progress indicator near the subtitle area itself (not a full-screen blocking spinner) so users can keep watching while subtitles generate.
- **Subtitle styling controls**: size, color, background opacity — exposed in Settings and/or a quick-access overlay during playback, since readability varies a lot by content and personal preference.
- **Error state**: a small, dismissible inline message ("Couldn't reach subtitle server — check your connection or backend status") rather than an intrusive dialog that interrupts playback.

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
