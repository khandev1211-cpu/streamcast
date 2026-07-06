# 07 — IPTV Integration

## What "IPTV support" means concretely

Two common ways users bring IPTV content into a player, both supported here:

1. **M3U/M3U8 playlist** — a plain-text file listing channel names, logos, group/category tags, and stream URLs.
2. **Xtream Codes API** — a login-based system (host, username, password) used by many IPTV providers, returning channel/category/EPG data via REST/JSON rather than a static playlist file.

## M3U/M3U8 parsing

A typical M3U entry looks like:

```
#EXTINF:-1 tvg-id="channel.id" tvg-logo="https://.../logo.png" group-title="News",Channel Name
https://example.com/stream/channel.m3u8
```

Parsing responsibilities:
- Extract `tvg-id` (for EPG matching), `tvg-logo` (channel icon), `group-title` (category), and the display name.
- Extract the stream URL on the following line.
- Handle malformed or partial entries gracefully — real-world playlists from various providers are inconsistent; the parser should skip bad entries rather than fail the whole import.
- Support both remote playlist URLs (fetch over HTTP) and local `.m3u` file uploads.

## Xtream Codes API

Rough shape of the (unofficial but widely used) API:

```
GET http://{host}/player_api.php?username={u}&password={p}&action=get_live_categories
GET http://{host}/player_api.php?username={u}&password={p}&action=get_live_streams&category_id={id}
GET http://{host}/player_api.php?username={u}&password={p}&action=get_short_epg&stream_id={id}
```

- Categories and streams come back as JSON; the app maps these into the same internal `Channel` model used for M3U-sourced channels, so downstream code (channel list UI, favorites, playback) doesn't need to know which method sourced the data.
- Credentials (host/username/password) are stored securely (see `19-SECURITY-PRIVACY.md`) since they're effectively account credentials for a paid service in most cases.

## EPG (Electronic Program Guide)

- Typically delivered as **XMLTV** — an XML format listing programs per channel with start/end times and titles/descriptions.
- Not all providers supply this; the app should treat EPG as optional metadata, degrading gracefully to "no program info available" rather than treating its absence as an error.
- EPG data can be large (full weekly schedules across hundreds of channels) — consider fetching/parsing it lazily (only for channels currently visible) rather than parsing an entire XMLTV file upfront.

## Unified `Channel` model

Regardless of source (M3U or Xtream), channels normalize into:

```kotlin
data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val category: String?,
    val streamUrl: String,
    val epgChannelId: String?,   // for matching against EPG data, nullable
    val sourceId: String,        // which IptvSource this belongs to
    val isFavorite: Boolean
)
```

## Multiple sources

- A user can add several IPTV sources (e.g., two different provider logins, or a provider login plus a manually-added M3U playlist).
- Channels are tagged with their `sourceId` so the UI can filter/group by source if the list gets large, and so re-syncing one source doesn't affect channels from another.

## Sync & refresh behavior

- Playlists and Xtream channel lists aren't static forever — providers update them. A manual "refresh" action re-fetches and re-parses, updating the local channel cache (Room) rather than requiring the user to re-add the source from scratch.
- Consider surfacing "last synced" timestamp per source so users know if their list might be stale.

## Error handling specific to IPTV

- Invalid/unreachable playlist URL → clear error message, not a silent empty list.
- Xtream login failure (bad credentials, expired subscription) → surface the actual failure reason where the API provides one, rather than a generic "something went wrong."
- Individual dead channel links within an otherwise valid playlist shouldn't block the rest of the list from loading — handle per-channel playback failures at play-time, not import-time.
