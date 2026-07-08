# 15 — Navigation & Screen Inventory

## Navigation structure

**Updated to match the actual MX Player reference screenshots** (see `14-UI-UX-DESIGN-SYSTEM.md` → "Exact Screen-by-Screen Reference"): MX Player uses a **bottom navigation bar**, not top tabs. This supersedes the earlier top-tab description below wherever they conflict.

Single-activity app using Compose Navigation, with a persistent **bottom nav bar** mirroring MX Player's own (Local / Music / Transfer / Me), adapted for this app as:

- **Local** — local file library (folders + videos), with IPTV and Live surfaced as segmented sub-tabs at the top of this screen (Videos | IPTV | Live), matching how MX Player itself uses top segmented tabs *within* a bottom-nav destination (e.g., its own Video/Audio split).
- **Music** — audio-only view of the local library.
- **Screen** — replaces MX Player's "Transfer" tab; this app's dedicated screen-related destination (casting/display, not Wi-Fi file transfer).
- **Me** — settings/profile entry point (VPS config, subtitle defaults, storage management).

```
MainActivity
└── NavHost
    ├── LocalScreen (bottom nav tab)
    │   ├── top segmented control: Videos | IPTV | Live
    │   │   ├── VideosSubTab → folder/grid browsing
    │   │   ├── IptvSubTab → AddIptvSourceScreen, channel list
    │   │   └── LiveSubTab → AddLiveUrlScreen, channel list
    │   └── PlayerScreen (full-screen, on any item tap)
    ├── MusicScreen (bottom nav tab)
    │   └── PlayerScreen (full-screen, on file tap)
    ├── ScreenScreen (bottom nav tab) — casting/display features
    └── MeScreen (bottom nav tab)
        ├── VpsConfigScreen
        ├── SubtitleDefaultsScreen
        └── StorageManagementScreen
```

## Screen inventory

### `LocalScreen` (bottom nav: "Local")
- Top bar: back/menu icon, title, right-aligned folder/browse icon, search icon, layout-toggle icon (grid/list) — per `14-UI-UX-DESIGN-SYSTEM.md` exact reference.
- Top segmented control: **Videos | IPTV | Live** — this is where this app's IPTV and Live features live, since MX Player's own bottom nav has no room for them.
- `VideosSubTab`: Folders section + Videos section, folder-first grid browsing (see design doc for exact row layout). Tapping an item opens `PlayerScreen`.
- `IptvSubTab`: list of configured `IptvSource`s; tapping one shows its channel list (grouped by category). "Add Source" action → `AddIptvSourceScreen`. Search and favorites filter.
- `LiveSubTab`: list of user-added live "channels." "Add Live URL" action → `AddLiveUrlScreen`.
- Each sub-tab keeps its own scroll/filter state when switching, rather than resetting.

### `AddIptvSourceScreen`
- Form: source type toggle (M3U vs Xtream), relevant input fields per type.
- Validation feedback (e.g., "couldn't reach playlist URL") before saving.

### `AddLiveUrlScreen`
- Single URL input, optional name field, validation check before saving (see `08-LIVE-STREAMS.md`).

### `MusicScreen` (bottom nav: "Music")
- Audio-only filtered view of the local library, same folder/list patterns as `VideosSubTab` but audio files only.
- Tapping an item opens `PlayerScreen` in audio-focused mode.

### `PlayerScreen` (shared across all content types — local video, local audio, IPTV, live)
- Full-screen surface, no bottom nav visible during playback.
- Top bar: back arrow, title, right icon row (playing-queue, music, equalizer, decoder badge, three-dot overflow) — exact spec in `14-UI-UX-DESIGN-SYSTEM.md`.
- Gesture zones: left-half vertical swipe = brightness, right-half vertical swipe = volume, double-tap left/right = seek ±10s.
- Quick-tool icon row + expandable full quick-tools row (customizable via Shortcuts panel) — see design doc for the complete icon list.
- Bottom control row: lock, prev, play/pause, next, fullscreen/expand, screen-output icon.
- Three-dot overflow opens the full grid menu (Playing Queue, Aspect Ratio, Display Settings, Bookmark, Cut, Favourite, Add To Playlist, Information, Share, Network Stream, Tutorial, More) plus Video Display / Shortcuts toggles.
- Subtitle generation flow (this app's addition, not in MX Player): language picker → loading state → rendered subtitle overlay → error state if applicable, reachable from the subtitle icon within the quick-tools row.
- **Landscape orientation**: distinct full-bleed layout (not a stretched portrait) — full-height gesture zones, single-line title, repositioned lock icon (left-edge, vertically centered), wider-spaced bottom controls. Full spec in `14-UI-UX-DESIGN-SYSTEM.md` → "Player screen — landscape orientation".

### `ScreenScreen` (bottom nav: "Screen")
- Replaces MX Player's "Transfer" tab. Houses this app's casting/display-related functionality rather than Wi-Fi file transfer.
- Exact feature scope TBD — flagged here as a placeholder destination until casting/display requirements are defined in more detail.

### `MeScreen` (bottom nav: "Me")
- Entry points to `VpsConfigScreen`, `SubtitleDefaultsScreen`, `StorageManagementScreen`.
- Theme toggle (if/when light mode is added).

### `VpsConfigScreen`
- VPS endpoint URL field, API key field (masked input).
- "Test connection" action hitting the VPS `/health` endpoint (see `13-NETWORKING-API-CONTRACTS.md`) with clear success/failure feedback.

### `SubtitleDefaultsScreen`
- Default target language.
- Default subtitle style (size, color, background opacity) with a live preview.

### `StorageManagementScreen`
- Cached subtitle count/size, with a "clear cache" action.
- Playback history clear action.

## Deep linking / state restoration

- `PlayerScreen` should support process-death state restoration (remember which `MediaSource` was playing and at what position) so backgrounding the app and returning doesn't lose playback context — particularly relevant given video playback can be long-running and Android may kill backgrounded processes under memory pressure.

## Navigation between tabs during playback

- Playback (especially audio-only/background mode, later phase) should be able to continue while the user navigates back to browse `LocalScreen` or `MusicScreen` — this requires playback state to live above the `PlayerScreen` composable itself (e.g., in a shared/app-level ViewModel or service) rather than being torn down when the screen is left.
- Switching between bottom-nav tabs, and between the Videos/IPTV/Live segmented sub-tabs within `LocalScreen`, should preserve each one's own scroll position and filter/search state — a user checking IPTV mid-scroll and returning to Videos shouldn't lose their place in either.