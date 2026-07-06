# 19 — Security & Privacy

## Sensitive data in this app

- **IPTV credentials** (Xtream Codes username/password) — effectively account credentials for what's often a paid service.
- **VPS API key** — grants access to the developer's personal subtitle inference server.
- **Audio content sent for transcription** — potentially private/personal video or audio content leaving the device.
- **Local file paths/library contents** — not especially sensitive on their own, but still user data worth handling carefully.

## Storing IPTV credentials

- Never store Xtream username/password in plain text in Room. Use Android's **EncryptedSharedPreferences** or Room with a SQLCipher-backed database (SQLCipher is heavier; EncryptedSharedPreferences may be sufficient if credential storage is simple key-value rather than relational).
- If storing within Room for relational convenience (joining against `Channel`/`IptvSource`), encrypt the username/password fields at the application layer before insertion, decrypting only when needed for an API call.

## Storing the VPS API key

- Store in EncryptedSharedPreferences, not in a plain settings file or, worse, hardcoded in source (hardcoding is especially risky since this key grants access to the developer's own server).
- Never log the API key (see `17-ERROR-HANDLING-LOGGING.md`) — redact it even in debug logging.

## Transport security

- All communication with the VPS **must** be over HTTPS/TLS — audio content and credentials should never transit in plaintext. Since the VPS is self-hosted, this means setting up a real TLS certificate (e.g., via Let's Encrypt) rather than skipping it because "it's just my own server" — the traffic still crosses the public internet.
- IPTV/Xtream API calls: use HTTPS where the provider supports it; if a provider only offers HTTP, that's a provider-side limitation worth being aware of but not something the app can fully mitigate beyond warning the user if desired.

## User-supplied URL handling (live streams)

- Validate URL scheme before passing to the player — only allow expected schemes (`http`, `https`, `rtmp`) rather than blindly accepting anything typed in.
- Be cautious of redirect chains from user-supplied URLs leading somewhere unexpected; Media3's own handling covers most of this, but don't add custom logic that blindly follows arbitrary redirects without any scheme validation.

## Android platform-level considerations

- **Scoped storage**: local library scanning must respect Android's scoped storage rules (no broad `MANAGE_EXTERNAL_STORAGE` unless genuinely justified) — use `MediaStore` APIs or Storage Access Framework for accessing user media, respecting user privacy and Play Store policy.
- **Permissions**: request only what's needed (storage/media read access, network access) — avoid requesting broad permissions "just in case."
- **Backup rules**: ensure Android's auto-backup doesn't inadvertently back up encrypted credential stores in a way that could be restored insecurely on another device — configure backup rules explicitly rather than relying on defaults.

## Privacy considerations around subtitle generation

- Audio from the user's local files or live streams is sent to the developer's own VPS for processing — since this is a personal project (not a multi-user public service), this is more of a "know what you're building" consideration than a strict compliance one, but still worth being transparent about in-app (e.g., a brief note in Settings near the VPS config: "Audio is sent to your configured server for subtitle generation") so it's never a surprise to whoever ends up using the app.
- If this app is ever shared with others (not just the developer), this becomes a genuine privacy disclosure requirement — worth keeping in mind if distribution plans change later.

## Dependency security

- Keep third-party libraries (Media3, Retrofit, Room, etc.) reasonably up to date to pick up security patches — not a one-time setup concern but an ongoing maintenance item.

## Summary checklist

- [ ] IPTV credentials encrypted at rest
- [ ] VPS API key encrypted at rest, never logged
- [ ] All VPS communication over HTTPS with a valid certificate
- [ ] User-supplied live URLs validated by scheme before use
- [ ] Scoped storage respected for local media access
- [ ] Minimal permission requests
- [ ] In-app transparency about audio being sent externally for subtitle generation
