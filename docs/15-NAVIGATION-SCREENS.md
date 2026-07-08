# 15 — Navigation & Screen Inventory

## Navigation structure

**Updated to match the actual MX Player reference screenshots** (see `14-UI-UX-DESIGN-SYSTEM.md` → "Exact Screen-by-Screen Reference"): MX Player uses a **bottom navigation bar**, not top tabs. This supersedes the earlier top-tab description below wherever they conflict.

Single-activity app using Compose Navigation, with a persistent **bottom nav bar** mirroring MX Player's own (Local / Music / IPTV / Me):

- **Local** — local file library (folders + videos).
- **Music** — audio-only view of the local library.
- **IPTV** — unified destination for IPTV playlists and user-added Live URLs.
- **Me** — settings/profile entry point (VPS config, subtitle defaults, storage management).

```
MainActivity
└── NavHost
    ├── LocalScreen (bottom nav tab) → folder/grid browsing
    │   └── PlayerScreen (full-screen, on any item tap)
    ├── MusicScreen (bottom nav tab)
    │   └── PlayerScreen (full-screen, on file tap)
    ├── IptvScreen (bottom nav tab) → IPTV sources, Live URLs
    └── MeScreen (bottom nav tab)
        ├── VpsConfigScreen
        ├── SubtitleDefaultsScreen
        └── StorageManagementScreen
```

## Screen inventory

### `LocalScreen` (bottom nav: "Local")
- Top bar: back/menu icon, title, right-aligned folder/browse icon, search icon, layout-toggle icon (grid/list) — per `14-UI-UX-DESIGN-SYSTEM.md` exact reference.
- Folders section + Videos section, folder-first grid browsing. Tapping an item opens `PlayerScreen`.

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

### `IptvScreen` (bottom nav: "IPTV")
- Unified destination for IPTV playlists and user-added live streams.
- List of configured `IptvSource`s; tapping one shows its channel list.
- Choice of `AddIptvSourceScreen` or `AddLiveUrlScreen` actions.

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