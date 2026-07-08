# 15 — Navigation & Screen Inventory

## Navigation structure

Single-activity app using Compose Navigation, featuring a **bottom navigation bar** for the four primary destinations (Video / Audio / IPTV / Profile), plus a full-screen player that takes over the whole screen (no nav bar visible) during playback.

```
MainActivity
└── NavHost
    ├── MainScreen (bottom nav: Video | Audio | IPTV | Profile)
    │   ├── VideoTab (Local video files)
    │   │   └── PlayerScreen (full-screen, on file tap)
    │   ├── AudioTab (Local audio files)
    │   │   └── PlayerScreen (full-screen, on file tap)
    │   ├── IptvTab (IPTV channels & Live streams)
    │   │   ├── AddIptvSourceScreen
    │   │   ├── AddLiveUrlScreen
    │   │   └── PlayerScreen (full-screen, on channel tap)
    │   └── ProfileTab (Settings & Configuration)
    │       ├── VpsConfigScreen
    │       ├── SubtitleDefaultsScreen
    │       └── StorageManagementScreen
```

This ensures critical navigation is always reachable at the bottom of the screen, while keeping the content-specific logic decoupled.

## Screen inventory

### `MainScreen`
- Hosts the bottom navigation bar (Video | Audio | IPTV | Profile).
- Each tab keeps its own scroll/filter state when switching tabs, rather than resetting.

### `VideoTab`
- Grid of scanned local video files, grouped by device folder by default (folder-first browsing); toggle to a flat compact list.
- Manual "add file" action (system file picker).
- Tapping an item opens `PlayerScreen`.

### `AudioTab`
- List/Grid of scanned local audio files, grouped by album or folder.
- Playback controls integration (mini-player when navigating).
- Tapping an item opens `PlayerScreen`.

### `IptvTab`
- Unified destination for IPTV playlists and user-added live streams.
- List of configured `IptvSource`s; tapping one shows its channel list (grouped by category).
- "Add Source" action → choice of `AddIptvSourceScreen` or `AddLiveUrlScreen`.
- Search and favorites filter across all channels.

### `AddIptvSourceScreen`
- Form: source type toggle (M3U vs Xtream), relevant input fields per type.
- Validation feedback (e.g., "couldn't reach playlist URL") before saving.

### `LiveTab`
- List of user-added live "channels."
- "Add Live URL" action → `AddLiveUrlScreen`.

### `AddLiveUrlScreen`
- Single URL input, optional name field, validation check before saving (see `08-LIVE-STREAMS.md`).

### `PlayerScreen` (shared across all three content types)
- Full-screen video surface, no tabs/nav chrome visible during playback.
- Gesture zones: left-half vertical swipe = brightness, right-half vertical swipe = volume, double-tap left/right = seek ±10s (MX-Player-style — see `14-UI-UX-DESIGN-SYSTEM.md`).
- Minimal auto-hiding overlay: play/pause, seek bar, prev/next, lock icon.
- Single "more options" corner icon opening a sheet for: subtitle language/generation, subtitle styling, audio track, playback speed, and (IPTV only) EPG "now/next" details.
- Subtitle generation flow: language picker → loading state → rendered subtitle overlay → error state if applicable.

### `ProfileTab`
- Entry points to `VpsConfigScreen`, `SubtitleDefaultsScreen`, `StorageManagementScreen`.
- User profile summary (if applicable) and app version info.
- Theme toggle and general app settings.

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

- Playback (especially audio-only/background mode) should be able to continue while the user navigates between the Video, Audio, or IPTV tabs — this requires playback state to live above the `PlayerScreen` composable itself (e.g., in a shared/app-level ViewModel or service) rather than being torn down when the screen is left.
- Switching between `VideoTab` / `AudioTab` / `IptvTab` / `ProfileTab` should preserve each tab's own scroll position and filter/search state — a user checking IPTV mid-scroll and returning to Video shouldn't lose their place in either.
