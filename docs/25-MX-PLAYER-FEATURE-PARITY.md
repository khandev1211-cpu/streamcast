# 25 — MX Player Feature Parity Checklist

## Purpose

MX Player is the explicit UX/feature reference for this app. This doc is a feature-by-feature checklist of what MX Player does, so nothing gets missed while building — with a note on what's kept identical, what's adapted, and what this app adds on top that MX Player doesn't have at all (IPTV, live URLs, AI subtitles).

**Note on scope**: this is about matching interaction patterns and feature depth, not copying MX Player's actual logo, brand name, or source code — those stay MX Player's. Everything below is a description of *behavior*, to be implemented independently.

---

## Home / Library screen

| MX Player feature | Plan for this app |
|---|---|
| Top segmented tabs (Video / Audio) | Top tabs: Library / IPTV / Live (adapted to this app's three content types) |
| Folder-first grid browsing with thumbnails | Same — folder grid with auto-generated video thumbnails |
| List view toggle | Same |
| Search across library | Same |
| Sort options (name, date, size, duration) | Same |
| "Last played" quick-resume row at top | Same — surfaced at top of Library tab |
| Long-press for multi-select (delete, share, etc.) | Same, adapted to this app's actions (delete from library, add to favorites) |
| Hide/lock private folders (passcode-protected) | Same — a "Private Folder" concept, passcode-gated, hides selected folders from the main grid |

## Player screen — controls

| MX Player feature | Plan for this app |
|---|---|
| Tap to show/hide controls, auto-hide after ~3s | Same |
| Bottom bar: play/pause, seek bar, current/total time | Same — Seekbar with toggleable Time Remaining; Playback row with Prev/Rew/Play/FF/Next |
| Top bar: back button, title, track selection, decoder, more-options icon | Same — Back, Title, Audio/Subtitle toggles, Decoder button, Menu |
| Lock icon (disables all touch except unlock) | Same — Positioned in Bottom Left corner |
| Prev/next (for folder/playlist navigation) | Same, applies to Library tab and IPTV channel-list navigation |
| Bottom-right: aspect ratio / zoom toggle (fit, fill, crop, stretch) | Same |
| Bottom-left or corner: subtitle toggle icon | Same, but opens this app's AI subtitle flow in addition to any existing subtitle track |

## Player screen — gestures

| MX Player feature | Plan for this app |
|---|---|
| Swipe left half vertically = brightness | Same |
| Swipe right half vertically = volume | Same |
| Double-tap left/right = seek ±10s | Same |
| Single horizontal swipe = seek scrub with preview time | Same |
| Pinch to zoom / double-tap-drag to resize | Same |
| Long-press = playback speed boost (temporary 2x while held) | Same — a nice, low-effort addition once base gestures work |

## Playback capabilities

| MX Player feature | Plan for this app |
|---|---|
| Hardware + software decoder toggle | Same — Media3 supports both; expose a toggle in Settings/more-options for compatibility troubleshooting |
| Multiple audio track selection | Same, via Media3's track selection APIs |
| Multiple subtitle track selection (external + embedded) | Same, alongside this app's AI-generated subtitle option — both listed together in the subtitle picker |
| Subtitle sync offset adjustment (+/- ms) | Same — important since both externally loaded and AI-generated subtitles can drift slightly |
| Subtitle styling (size, color, background) | Same, already planned (see `14-UI-UX-DESIGN-SYSTEM.md`) |
| Playback speed control (0.5x–3x typically) | Same |
| Equalizer / audio gain boost | Same — a basic EQ + gain boost for quiet audio files, standard MX Player feature |
| Sleep timer | Same — stop playback after a set duration |
| A-B repeat (loop a section) | Same, useful for language learners re-watching a subtitle segment — nice synergy with the AI subtitle feature |
| Resume playback prompt ("Resume from 12:34?") | Same |
| Background/audio-only playback | Same, already planned (see `06-PLAYBACK-ENGINE.md`) |
| Floating/pop-up window (draggable, resizable, over other apps) | Same — MX Player's signature feature, Phase 4 item here too |

## Network & sources

| MX Player feature | Plan for this app |
|---|---|
| Network streaming: SMB, FTP, HTTP/HTTPS URL, UPnP/DLNA discovery | Adapted — this app's "Live" tab covers HTTP(S)/HLS/DASH/RTMP URLs; SMB/FTP local-network file browsing is a reasonable Phase 4 addition if useful, though it's a different use case (network file shares vs. streaming URLs) worth evaluating on its own merits rather than assuming parity is required here |
| IPTV/playlist import | MX Player has limited/no native IPTV support — this is one of this app's actual differentiators, not a parity item |

## What this app adds beyond MX Player (the actual differentiators)

- **AI-generated subtitles in any language**, for any video, via self-hosted Whisper — MX Player has no equivalent; it only plays existing subtitle files.
- **Native IPTV support** (M3U/Xtream/EPG) — MX Player doesn't offer this as a core feature.
- **Unified live-stream + IPTV + local library** in one coherent model, rather than local playback being the sole focus.

## Explicit non-goals (won't chase parity on these)

- MX Player's ad-supported free tier / subscription model — irrelevant to this app's current scope and not a UX pattern worth copying.
- Their exact icon set/branding — build original iconography that fits this app's own color/design system (`14-UI-UX-DESIGN-SYSTEM.md`), just following the same *placement and behavior* conventions.
- Kids-lock / parental-control specifics beyond the basic private-folder passcode concept, unless later found to be genuinely useful.

## Suggested build order for parity features

1. Core gestures + control layout (biggest "feels like MX Player" impact for least effort)
2. Aspect ratio/zoom, subtitle sync offset, playback speed (small, high-value additions)
3. Resume prompt, sleep timer, A-B repeat (nice-to-have polish)
4. Equalizer, hardware/software decoder toggle (more advanced, can wait for Phase 4)
5. Floating/pop-up window, private folders (most complex, explicitly Phase 4)
