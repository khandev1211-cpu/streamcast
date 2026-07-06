# 21 — CI/CD & Build Setup

## Build variants

Recommended Gradle build types/flavors:

- **debug** — verbose logging enabled, points to a configurable/local VPS endpoint (easy to swap during development), unsigned.
- **release** — minimal logging, signed with a release keystore, points to the production VPS endpoint by default (still overridable in Settings, since the VPS is self-hosted and its address may change).

Consider a `staging` flavor if a separate test VPS instance is ever stood up, so app builds can point at a test backend without touching production VPS config.

## Recommended CI pipeline (e.g., GitHub Actions)

```
on: [push, pull_request]

jobs:
  build-and-test:
    steps:
      - checkout
      - setup JDK + Android SDK
      - run: ./gradlew assembleDebug
      - run: ./gradlew testDebugUnitTest
      - run: ./gradlew lint
      # optional, once UI tests exist:
      - run: ./gradlew connectedDebugAndroidTest  # requires emulator/device runner
```

- Keep CI fast by running unit tests and lint on every push/PR; reserve slower instrumented/UI tests for either a nightly run or pre-release checks, rather than blocking every commit.
- Fail the build on lint errors related to security or correctness (not just style warnings), to catch issues like unencrypted credential storage early (see `19-SECURITY-PRIVACY.md`).

## Static analysis

- **ktlint** or **detekt** for Kotlin style/code-quality consistency across modules.
- Android Lint for platform-specific issues (permission usage, deprecated API usage, resource issues).

## Versioning

- Semantic-ish versioning (`major.minor.patch`) for `versionName`, with `versionCode` auto-incremented per build (e.g., based on CI build number) rather than manually bumped, to avoid Play Store upload conflicts.

## Release process (once ready to distribute, even informally)

1. Bump version, tag the release commit.
2. CI builds a signed release APK/AAB.
3. Manual smoke test pass (see `20-TESTING-STRATEGY.md` manual testing section) — especially real playback and real subtitle generation against the actual VPS, since these are the hardest things to fully verify in CI.
4. Distribute via Play Store internal testing track, direct APK sharing, or whichever distribution method fits given this is currently a personal/small-scale project rather than a public store listing (that decision can be revisited later).

## VPS deployment (separate from the Android CI pipeline)

- The Python/FastAPI subtitle service (see `10-VPS-WHISPER-SETUP.md`) has its own deploy process — not part of Android CI, but worth having *some* repeatable process (even a simple deploy script that pulls latest code, restarts the systemd service) rather than manual ad-hoc VPS changes, so backend updates don't risk unexpected downtime for the app's subtitle feature.
- Consider a basic health-check step after any VPS deploy (hit `/health`) before considering the deploy successful.

## Secrets management

- API keys, keystore passwords, and any VPS credentials used in CI (if CI ever needs to deploy to the VPS) should live in the CI platform's encrypted secrets store (e.g., GitHub Actions secrets), never committed to the repository in plain text.
