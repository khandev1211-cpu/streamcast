# 16 — State Management

## General approach

Standard Android MVVM with unidirectional data flow: ViewModels expose `StateFlow`s of immutable UI state; Compose screens collect and render them; user actions call ViewModel functions, which update state or trigger use cases, never the other way around.

## Playback state (shared across features)

Because playback needs to persist across navigation (see `15-NAVIGATION-SCREENS.md`) and multiple screens/components need to observe it (player controls, subtitle renderer, mini-player if added later), playback state lives above individual feature ViewModels:

```kotlin
class PlaybackStateHolder @Inject constructor(
    private val playerManager: PlayerManager
) {
    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val currentSource: StateFlow<MediaSource?> = playerManager.currentSource
}
```

This is scoped at the application/activity level (via Hilt) rather than per-screen, so it survives navigation between tabs.

## Feature-level state

Each feature module's ViewModel owns state relevant only to that screen:

```kotlin
data class LibraryUiState(
    val files: List<LocalMedia> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun loadFiles() { /* ... */ }
}
```

Same pattern applies to `IptvViewModel`, `LiveViewModel`, `SettingsViewModel`.

## Subtitle state (its own slice, given complexity)

Subtitle generation has enough distinct states to warrant a dedicated sealed class rather than a handful of booleans:

```kotlin
sealed class SubtitleUiState {
    object Idle : SubtitleUiState()
    object SelectingLanguage : SubtitleUiState()
    data class Generating(val language: String) : SubtitleUiState()
    data class Active(val cues: List<SubtitleCue>, val language: String) : SubtitleUiState()
    data class Error(val message: String) : SubtitleUiState()
}
```

`SubtitleViewModel` transitions between these based on user actions and repository responses; `PlayerScreen` renders different UI (button, loading spinner, subtitle overlay, error banner) purely based on which state it's in — no scattered boolean flags to keep in sync.

## Handling live vs one-shot subtitle state differently

- **One-shot (local)**: `Generating` → single response → `Active` with the full cue list, or `Error`.
- **Rolling (IPTV/live)**: `Generating` → `Active` with an initial partial cue list → repeated background updates append/replace cues in `Active` as new chunks come back, without leaving the `Active` state — the UI shouldn't flicker back to a loading state every 20–30 seconds just because a new chunk is being fetched in the background.

## Error state propagation

- Network errors (VPS unreachable, IPTV fetch failed) surface through the same `NetworkResult` wrapper (see `13-NETWORKING-API-CONTRACTS.md`) at the repository layer, translated into feature-specific UI state (`LibraryUiState.error`, `SubtitleUiState.Error`, etc.) rather than leaking raw exceptions or network types into the UI layer.

## Testing implication

- Because state is modeled as explicit sealed classes/data classes rather than loose flags, ViewModel unit tests can assert on exact state transitions (e.g., "after calling `generateSubtitles()`, state goes `Idle` → `Generating` → `Active`") without needing to stand up the real player or network stack — see `20-TESTING-STRATEGY.md`.
