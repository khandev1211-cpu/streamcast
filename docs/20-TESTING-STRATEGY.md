# 20 — Testing Strategy

## Testing pyramid for this project

Given the mix of UI, playback, networking, and an external VPS dependency, testing should lean heavily on unit and integration tests for logic-heavy layers (repositories, ViewModels, parsers), with a smaller set of UI and manual tests for playback itself, since actual video/audio playback is hard to meaningfully unit test.

## Unit tests

### ViewModels
- Test state transitions using fake repositories (see below), asserting exact `StateFlow` emissions for a given sequence of actions — e.g., `SubtitleViewModel`: `Idle` → `Generating` → `Active`, and the corresponding `Error` path when the fake repository returns a failure.

### Repositories
- Test against fake/mock data sources (fake Room DAOs, fake Retrofit services via MockWebServer) rather than real network/database — fast, deterministic, and don't require a running VPS or real IPTV provider.

### Parsers (M3U, Xtream JSON, XMLTV)
- These are prime unit-testing targets: feed in real-world sample playlists/responses (including intentionally malformed ones) and assert correct parsing or graceful handling of bad entries. Because IPTV data in the wild is inconsistent (see `07-IPTV-INTEGRATION.md`), a good test suite here should include messy, non-ideal sample data, not just clean textbook examples.

### Subtitle cue conversion & timing logic
- Test the conversion from VPS segment responses into renderable cues, including edge cases like overlapping segments, out-of-order timestamps (shouldn't happen but defensively worth testing), and cue lookup for a given playback position.

## Integration tests

- **VPS API contract tests**: using MockWebServer to simulate the VPS's `/transcribe` and `/health` responses (success, various error codes, timeouts) and verifying the app's `SubtitleRepository` handles each correctly — this can be developed and run without a live VPS, and should be updated whenever the actual VPS API contract changes (see `13-NETWORKING-API-CONTRACTS.md`).
- **Room database tests**: using an in-memory Room database to verify DAOs, migrations, and entity relationships behave as expected.

## UI tests (Compose)

- Focused on critical flows rather than exhaustive screen coverage: adding an IPTV source, adding a live URL, triggering subtitle generation and seeing the loading → active/error states render correctly.
- Use Compose testing APIs with fake ViewModels/state rather than driving real network calls in UI tests, to keep them fast and independent of the VPS being up.

## Manual/exploratory testing (necessary given the domain)

- **Real playback testing** across actual local files (various formats/codecs), a real IPTV provider account, and various pasted live stream URLs — this is hard to fully automate and should be part of the regular manual testing routine, especially before releases.
- **Real subtitle generation testing** against the actual VPS, across different source languages and target languages, to validate real-world latency and translation quality (see `11-TRANSLATION-STRATEGY.md`) — automated tests with mocked VPS responses can't catch actual model accuracy issues.
- **Network condition testing** — simulate poor connectivity (Android's network throttling tools, or physically testing on weak Wi-Fi) to observe how gracefully IPTV/live playback and subtitle generation degrade.

## What not to over-invest in early

- Pixel-perfect UI screenshot testing before the design has stabilized — premature and likely to be thrown away as the UI evolves during Phase 1–2.
- Full end-to-end automated tests against a live VPS — brittle (depends on VPS uptime) and slow; reserve real-VPS testing for manual/exploratory passes and occasional integration checks, not the primary CI test suite.

## Suggested test coverage priority order

1. M3U/Xtream/XMLTV parsers (high value, low effort, high real-world inconsistency risk)
2. Subtitle cue conversion & state machine logic
3. Repository layer with mocked network/DB
4. ViewModel state transitions
5. Critical-path Compose UI tests
6. Manual playback & real-VPS testing (ongoing, not a one-time pass)
