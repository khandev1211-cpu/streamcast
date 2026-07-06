# 08 — Live Streams (User-Added URLs)

## Scope

Distinct from IPTV-provider channels, this covers the case where a user directly pastes a stream URL they found elsewhere — an HLS link, a DASH manifest, or an RTMP address — and wants to play (and optionally save) it as a "channel" in the app.

## Supported protocols

| Protocol | Extension/format | Support level |
|---|---|---|
| HLS | `.m3u8` | Native via Media3 |
| DASH | `.mpd` | Native via Media3 |
| RTMP | `rtmp://` | Via Media3's RTMP extension library |

## Adding a live URL

1. User pastes a URL into an "Add Live Stream" input.
2. App performs a lightweight reachability/format check — attempt to prepare the media source via Media3 without fully committing to playback, to catch obviously broken or unsupported links before saving.
3. If valid, user optionally names it and saves it as a `Channel` (same underlying model as IPTV channels, distinguished by `sourceId` pointing to a synthetic "user-added" source rather than a real IPTV provider).
4. If invalid, show a clear error (unreachable, unsupported format) rather than saving a broken entry.

## Why reuse the `Channel`/`MediaSource` model

Treating user-added URLs as just another `Channel` (with no `category`/`EPG` data, since none exists) means:
- The same channel list UI, favorites system, and playback path work without special-casing.
- Subtitle generation works identically to IPTV channels — from the player's perspective, both are just live, non-cacheable, rolling-buffer sources.

## Differences from provider-based IPTV

- **No EPG** — there's no program guide for an arbitrary pasted URL, so "now/next" UI elements simply don't render for these entries.
- **No bulk import** — these are added one at a time by the user, not synced from a provider's full channel list.
- **Higher variability in reliability** — random URLs found online are more likely to go dead, be geo-restricted, or have inconsistent stream quality compared to a paid IPTV provider's infrastructure. The app should handle playback failures gracefully (clear "stream unavailable" messaging) since this is expected to happen more often here than with IPTV sources.

## Stream health considerations

- A "valid at add-time" URL can still go offline later. Consider an optional periodic health check (or simply handle failure gracefully at play-time) rather than assuming a saved URL will always work.
- No automatic removal of dead links — let the user decide whether to remove a channel that's stopped working; the app just needs to fail gracefully and clearly when playback doesn't succeed.

## Security note

- Since users can paste arbitrary URLs, treat all such input as untrusted: validate the URL scheme/format before passing it to the player, and don't blindly follow redirects to unexpected schemes. See `19-SECURITY-PRIVACY.md` for more on handling user-supplied URLs safely.
