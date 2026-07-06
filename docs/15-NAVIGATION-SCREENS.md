# 15 — Navigation & Screen Inventory

## Navigation structure

Single-activity app using Compose Navigation, with a persistent bottom navigation bar for the four top-level destinations, plus a full-screen player that overlays/replaces the bottom nav during playback.

```
MainActivity
└── NavHost
    ├── LibraryScreen (tab)
    │   └── PlayerScreen (full-screen, on file tap)
    ├── IptvScreen (tab)
    │   ├── AddIptvSourceScreen
    │   └── PlayerScreen (full-screen, on channel tap)
    ├── LiveScreen (tab)
    │   ├── AddLiveUrlScreen
    │   └── PlayerScreen (full-screen, on channel tap)
    └── SettingsScreen (tab)
        ├── VpsConfigScreen
        ├── SubtitleDefaultsScreen
        └── StorageManagementScreen
```

## Screen inventory

### `LibraryScreen`
- Grid/list of scanned local video & audio files.
- Manual "add file" action (system file picker).
- Tapping an item opens `PlayerScreen`.

### `IptvScreen`
- List of configured `IptvSource`s; tapping one shows its channel list (grouped by category).
- "Add Source" action → `AddIptvSourceScreen` (choose M3U URL/file or Xtream login).
- Search and favorites filter across all IPTV channels.

### `AddIptvSourceScreen`
- Form: source type toggle (M3U vs Xtream), relevant input fields per type.
- Validation feedback (e.g., "couldn't reach playlist URL") before saving.

### `LiveScreen`
- List of user-added live "channels."
- "Add Live URL" action → `AddLiveUrlScreen`.

### `AddLiveUrlScreen`
- Single URL input, optional name field, validation check before saving (see `08-LIVE-STREAMS.md`).

### `PlayerScreen` (shared across all three content types)
- Full-screen video surface.
- Auto-hiding controls overlay: play/pause, seek bar, speed, lock, subtitle button.
- EPG "now/next" strip — only rendered when `SourceMetadata` includes EPG info (IPTV only).
- Subtitle generation flow: language picker → loading state → rendered subtitle overlay → error state if applicable.

### `SettingsScreen`
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

- Playback (especially audio-only/background mode, later phase) should be able to continue while the user navigates back to browse the Library or IPTV tab — this requires playback state to live above the `PlayerScreen` composable itself (e.g., in a shared/app-level ViewModel or service) rather than being torn down when the screen is left.
