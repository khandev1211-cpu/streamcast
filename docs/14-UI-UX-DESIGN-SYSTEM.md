# 14 — UI/UX Design System

## Design direction

**Explicit reference point: MX Player.** Layout, navigation, and player-screen interaction patterns follow MX Player's proven conventions rather than inventing a new interaction model — this app adds AI subtitles and IPTV/live sources on top of a layout users already know how to use.

Dark-first, modern, minimal-chrome-during-playback. The player itself should feel like the star of the screen — controls fade away quickly, typography is clean, and navigation between Library/IPTV/Live/Subtitles/Settings is fast and unambiguous.

### What "MX Player style" means concretely here

- **Top tabs, not just bottom nav** — primary navigation (Library / IPTV / Live) sits as tabs at the top of the home screen, MX-Player-style, rather than relying only on bottom nav. Settings lives behind an overflow/menu icon rather than taking up a full tab slot.
- **Grid-first local library, grouped by folder** — local videos display as a thumbnail grid, grouped by device folder first (mirroring how MX Player surfaces "Video" by folder before flattening to one list). A grid/list toggle remains available.
- **Gesture-driven player screen** — swipe vertically on the left half of the screen for brightness, right half for volume; double-tap left/right to seek ±10s; pinch or double-tap-and-hold to resize/zoom video. These gestures work without any visible control, which is core to why MX Player's player screen feels fast.
- **Minimal always-visible controls, everything else tucked into a corner menu** — the visible overlay is just a seek bar, play/pause, and prev/next. Subtitle language, audio track, playback speed, and subtitle styling live behind a single "more options" icon (top-right corner, MX-Player-style) rather than spread across the main overlay.
- **Floating/pop-up window mode** — a resizable, draggable floating player window that persists over other apps, MX Player's signature feature. Treated as a Phase 4 polish item (see `22-ROADMAP.md`) since it requires overlay-window handling, but the player architecture (`06-PLAYBACK-ENGINE.md`) shouldn't preclude it later.

## Color

- **Base**: near-black background (not pure `#000000` — a very dark neutral, e.g., `#0E0E12`, reduces harsh contrast and OLED smearing artifacts while still feeling "dark mode").
- **Surface elevation**: slightly lighter dark tones for cards/sheets (channel lists, settings panels) to create depth without relying on shadows, which read poorly on dark backgrounds.
- **Accent color**: a single vibrant accent (e.g., a saturated blue or purple) used sparingly — for the play button, active nav item, and the "Generate Subtitles" call-to-action specifically, since that's the differentiating feature and deserves visual emphasis.
- **Semantic colors**: distinct, consistent colors for error states (stream failed, VPS unreachable) vs. informational states (buffering, syncing) — don't reuse the accent color for both success and error contexts.

## Typography

- A clean, modern sans-serif (system default like Roboto is fine, or a distinct choice like Inter if the app wants slightly more personality).
- Clear hierarchy: large bold titles for screen headers, medium weight for channel/file names, smaller regular weight for metadata (duration, category, "now playing" EPG text).

## Layout patterns

- **Top tab bar** (Library / IPTV / Live) as the primary navigation, MX-Player-style, with Settings reached via an overflow icon rather than a fourth tab.
- **Grid-first, folder-grouped local library** — thumbnails in a grid, grouped by folder by default; a toggle switches to a flat compact list for users who prefer scanning by filename.
- **Channel lists (IPTV/Live)** default to a list view (denser, more scannable for potentially hundreds of channels) with channel logos as small leading icons, category headers, and a grid toggle available for users who prefer browsing by logo.
- **Full-screen player** with auto-hiding controls and MX-Player-style gesture zones (see above) — tap to reveal controls, auto-hide after a few seconds of inactivity.
- **Corner "more options" menu** on the player screen — a single icon opening a sheet/panel with subtitle language, subtitle styling, audio track, playback speed, and (for IPTV) EPG details, keeping the main overlay uncluttered.

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

---

## Exact Screen-by-Screen Reference (from MX Player reference screenshots)

The following locks down precise layout details captured directly from MX Player screenshots, so the build matches pixel-for-pixel behavior, not just the general idea.

### Local Library screen (equivalent to MX Player's "Download"/folder browser)

**Top bar:**
- Back arrow (left)
- Screen title (e.g., "Local" / current folder name)
- Right-aligned icon row: folder/browse icon, search icon, layout-toggle icon (switches grid/list)

**Folders section:**
- Section label ("Folders")
- Each folder row: folder icon (with a small red badge showing unwatched/new item count when applicable), folder name, subtext line showing item count + total size (e.g., "6 folders · 90 GB")

**Videos section (below Folders):**
- Section label ("Videos")
- Each video row: left-aligned thumbnail (with duration badge overlaid bottom-left, e.g., "01:18"), title (up to 2 lines, truncated with ellipsis), subtext row showing file size + date (e.g., "5.3 MB · 22 Jun"), three-dot overflow menu icon on the far right of each row
- List is scrollable; a floating circular play button (bottom-right, accent-colored) appears as a persistent quick-play/resume action

**Bottom navigation bar (4 tabs):**
- **Local** (folder icon) — local library, current screen
- **Music** (music-note icon) — audio-only library view
- **Screen** (replaces MX Player's "Transfer" tab) — this app's dedicated screen for casting/screen-related functionality rather than MX Player's Wi-Fi file-transfer feature; icon should visually imply "screen/display" (e.g., a monitor or cast-style icon) rather than the transfer arrows MX Player uses
- **Me** (profile icon) — settings/profile entry point

### Player screen — top bar

- Back arrow (left)
- Title, up to 2 lines, truncated with ellipsis
- Right-aligned icon row, in order: playing-queue/cast-style icon, music-note icon, equalizer/mixer icon, decoder badge ("HW+" — indicates hardware decoding is active, tappable to toggle), three-dot overflow menu

### Player screen — quick-tool icon row (appears with controls)

A horizontal row of circular icon buttons directly below the top bar, left-to-right:
1. Settings/mixer icon (opens detailed playback settings)
2. Speed badge (shows current speed, e.g., "1X" — tap to cycle or open speed picker)
3. Screenshot/capture icon
4. Headphones icon (audio output / audio effect shortcut)
5. Rotate/mirror icon
6. A trailing `>` chevron — expands into the full quick-tools row (below)

### Player screen — expanded quick-tools row (after tapping the chevron)

A horizontally scrollable row of icon+label pairs, each a circular icon above a text label:
- Night Mode
- Customise Items
- Shuffle
- Loop
- Mute
- Sleep Timer
- A-B Repeat
- Mirror Mode
- Vertical Flip
- Audio Effect
- Equalizer
- Speed
- Screenshot
- Background (play) — continues off-screen, scrollable

This expanded row is itself user-customizable — see "Shortcuts" panel below.

### Player screen — subtitle + bottom controls

- Subtitle text renders directly above the seek bar, white text (as generated/loaded), no forced background box in the reference — but see `Accessibility` above: this app should still support an optional semi-transparent backing for readability, exposed as a style setting even if off by default to match the reference look.
- Seek bar: current time (left), total time (right), draggable scrub handle (accent-colored dot), progress fill in accent color, remainder in muted gray.
- Bottom control row (left to right): lock icon (locks touch/gestures), previous, play/pause (center, largest), next, expand/fullscreen icon, and a second icon at the far right for screen/display output (cast-style — pairs with the "Screen" bottom-nav tab concept rather than MX Player's own casting feature).

### Player screen — three-dot overflow menu (full-screen grid overlay)

Tapping the top-bar three-dot icon opens a translucent overlay over the video, top-right anchored, as a 4-column icon grid:

Row 1: Playing Queue, Aspect Ratio, Display Settings, Bookmark
Row 2: Cut, Favourite, Add To Playlist, Information
Row 3: Share, Network Stream, Tutorial, More

Below the grid, two toggle rows:
- **Video Display** — toggle (on/off)
- **Shortcuts** — toggle (on/off); when enabled, tapping it expands into the Shortcuts customization panel (below)

### Shortcuts customization panel

A checklist (two-column) of every quick-tool available for the customizable shortcut row, each with a checkbox to include/exclude it from the player's quick-tools row:

Screen Rotation, Playback Speed, Background Play, Loop, Mute, Shuffle, Equalizer, Audio Effect, Sleep Timer, A-B Repeat, Night Mode, Customise Items, Screenshot, Mirror Mode

All shown checked by default; unchecking an item removes it from the expanded quick-tools row on the player screen, letting users trim the row down to only what they use.

### Player screen — landscape orientation

The player screen must have a distinct landscape layout, not just a stretched portrait one — this is the orientation most video is actually watched in, so it needs its own explicit spec rather than being an afterthought.

- **Full-bleed video** — video fills the entire screen width and height (edge-to-edge, accounting for any device notch/cutout); no letterboxing beyond what the video's own aspect ratio requires.
- **Gesture zones extend full height** — the left-half/right-half swipe zones for brightness/volume (see gesture spec above) span the full screen height in landscape, not just a portion of it, since there's no bottom nav or other chrome competing for space.
- **Top bar** — same content as portrait (back arrow, title, playing-queue/music/equalizer icons, HW+ badge, three-dot overflow), stretched across the full width; title truncates to a single line in landscape rather than two, given the extra horizontal space.
- **Quick-tool row** — same icon set as portrait, positioned directly below the top bar, spread with slightly more spacing given the extra width; the expanded quick-tools row (Night Mode → Screenshot) scrolls horizontally the same way it does in portrait.
- **Lock icon** — repositioned to vertically centered on the left edge in landscape (rather than bottom-left as in portrait), since the bottom control row in landscape is more spread out and the lock icon benefits from being reachable independent of it.
- **Subtitle line** — centered horizontally with wider side margins than portrait (roughly matching the width of the bottom control row below it), sized slightly larger given the bigger canvas, still positioned just above the seek bar.
- **Seek bar and bottom controls** — same left-to-right order as portrait (lock, prev, play/pause, next, fullscreen/expand, screen-output icon), but spread across the full width with larger gaps between groups rather than the tightly-packed portrait arrangement.
- **No bottom nav, no folder/library chrome visible** — landscape playback is full-screen-only; rotating back to portrait (or pressing back) returns to whichever screen launched playback (Local/Music/etc.), consistent with the portrait `PlayerScreen` behavior described in `15-NAVIGATION-SCREENS.md`.
- **Auto-hide behavior unchanged** — controls fade in/out on tap exactly as in portrait; landscape doesn't change the timing or trigger, only the layout of what's shown.

An interactive landscape mockup of this layout exists alongside the portrait mockups (see the project's HTML prototype) — treat it as the literal reference the same way the portrait screenshots above are treated.

### Implementation note

This section supersedes the general "Layout patterns" description above wherever the two differ in specifics — treat this as the literal build reference, and the earlier sections as the rationale/principles behind it.