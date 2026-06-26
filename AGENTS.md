# AGENTS.md

## Project

Rovo is an Android TV streaming app (Kotlin, Jetpack Compose for TV, MVVM + Hilt). Two Gradle modules: `app` (main) and `playbackcore` (Media3/ExoPlayer wrapper). An external `assrender` module lives at `../assrender/assrender`.

## Build

```bash
./gradlew assembleDebug    # Windows: gradlew.bat assembleDebug
```

No other build, lint, or test commands are configured. There are no test files in the repo.

## Environment Requirements

- **JDK 17** — forced in `gradle.properties` to `C:/Program Files/Android/Android Studio/jbr`. If your JDK path differs, update `org.gradle.java.home` in `gradle.properties`.
- **compileSdk 36** — required by bundled ExoPlayer AARs. `android.suppressUnsupportedCompileSdk=36` is already set.
- **kapt** (not KSP) for Hilt and Room annotation processing.
- **Hilt 2.51.1** — declared manually in `build.gradle.kts`, not managed by the version catalog.

## Secrets

API keys are loaded from `local.properties` (gitignored). Required keys for full functionality:
- `tmdb.api_key`
- `TRAKT_CLIENT_ID`, `TRAKT_CLIENT_SECRET`
- `acra.url`, `acra.token`

These are injected into `BuildConfig` at compile time.

## Compose

- **Strong skipping mode** enabled in `app/build.gradle.kts` via `composeCompiler`.
- **Stability config** at `app/compose_stability_config.conf` — lists classes the Compose compiler treats as stable. When adding new domain models or entities that flow through Compose, add them here to avoid unnecessary recomposition.

## Modules

| Module | Purpose |
|---|---|
| `app` | Main application — UI, DI, data layer, domain models |
| `playbackcore` | Media3/ExoPlayer wrapper library. Exposes `api()` deps. Bundled native decoder AARs in `playbackcore/libs/` |
| `assrender` | External ASS/SSA subtitle renderer. Lives outside the repo at `../assrender/assrender` |

## Key Paths

- Entry: `app/src/main/java/com/rovo/app/MainActivity.kt`
- Application class: `app/src/main/java/com/rovo/app/RovoApplication.kt`
- DI modules: `app/src/main/java/com/rovo/app/di/`
- UI screens: `app/src/main/java/com/rovo/app/ui/` (home, player, details, addons, profiles, settings, search, theme, cast, studio, watchlist, navigation, utils)
- Data layer: `app/src/main/java/com/rovo/app/data/` (13 subpackages)
- ProGuard rules: `app/proguard-rules.pro` — keeps all `com.rovo.app.**` classes

## Agent Skills

The repo has OpenCode agent skills in `.agents/skills/` and `skills/` (android-clean-architecture, android-jetpack-compose, kotlin-testing, etc.). Use the `skill` tool to load relevant ones before Android/Kotlin tasks.

## Notes

- Debug builds get `.test` applicationId suffix (`com.rovo.app.test`).
- ExoPlayer AARs are committed as binary files in `playbackcore/libs/`. Do not attempt to download them.
- `lint { abortOnError = false }` — lint errors won't fail builds.
- `android.nonTransitiveRClass=false` — R classes are transitive (non-default).
