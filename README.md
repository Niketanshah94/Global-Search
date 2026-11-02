# QuickFind

An Android app that finds anything on your phone from one search bar: apps, contacts, files,
folders and system settings, plus a calculator and web search. Everything runs on the device.

"QuickFind" is a working name; check trademark and Play availability before release.

This repository currently implements **Phase 0 (project setup) and Phase 1 (free MVP)** of the
build specification. Phases 2–4 (on-device AI, Pro/cloud AI, launcher mode) are not started.

## Requirements

- Android Studio (latest stable) or the command line with **JDK 17+**
- Android SDK with platform **36** installed (`compileSdk`/`targetSdk` 36, `minSdk` 26)

## Setup

```bash
git clone <this repo>
cd Global-Search
# Point Gradle at your SDK if Android Studio has not created local.properties:
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Common tasks

| Task | Command |
|---|---|
| Unit tests (all modules) | `./gradlew test` |
| Fast matcher/ranking tests only (pure JVM) | `./gradlew :core:matching:test` |
| Lint | `./gradlew lint` |
| Release APK (R8 on, unsigned) | `./gradlew assembleRelease` |
| Room DAO + Compose UI tests (device/emulator) | `./gradlew connectedDebugAndroidTest` |
| Macrobenchmarks (device) | `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest` |
| Generate Baseline Profile (API 33+ device) | `./gradlew :app:generateReleaseBaselineProfile` |

CI (`.github/workflows/ci.yml`) runs `lint test assembleRelease` on every push.

## Module layout

```
app/              Application, manifest, widget, Quick Settings tile, notification, DI wiring
core/model/       SearchResult, SourceType, ResultAction (pure Kotlin)
core/matching/    Normalizer, fuzzy matcher, ranking, calculator (pure Kotlin, no Android)
core/data/        Room (history, file index + FTS4, pins, hidden), DataStore settings
core/sources/     One SearchSource per data type + the SearchEngine query pipeline
core/indexing/    MediaStore/walk/SAF file indexer, WorkManager jobs, observers
core/ui/          Shared Compose theme and glyphs (added; not in the spec's layout)
feature/search/   Search overlay UI, ViewModel, action executor, folder view
feature/settings/ Settings, onboarding, permission screens
benchmark/        Macrobenchmarks and Baseline Profile generator
```

`core/ai`, `core/billing` and `feature/launcher` arrive with Phases 2–4. The on-device embedding
model for meaning search lives in `core/sources/src/main/assets/universal_sentence_encoder.tflite`
(Universal Sentence Encoder, Apache 2.0, from MediaPipe's model storage).

## How search works

1. Each keystroke updates a `StateFlow`; `SearchEngine` uses `transformLatest`, so a new keystroke
   cancels the previous search.
2. In-memory (apps, contacts, settings) and instant (calculator, phone, URL, web) sources run
   immediately on `Dispatchers.Default`, and their results are shown at once.
3. Indexed sources (files) wait 80 ms (cancelled if the user keeps typing), query Room FTS4 with
   prefix `MATCH`, then re-score the top 200 with the fuzzy matcher. Their results merge in.
4. `ResultAssembler` adds history, frecency and type-bias boosts, removes hidden results,
   de-duplicates, picks the top hit and groups the rest into sections.

All ranking constants live in `core/matching/.../MatchWeights.kt` and `ranking/Ranking.kt`.

## Permissions

The app works with **no permissions**: apps, settings, calculator and web search need none.
Contacts, media and full file access are requested only when the user turns those sources on, each
after an explanation. The `INTERNET` permission is used only to download public exchange rates for
currency conversion (no user data is sent; Settings → Currency conversion turns it off). See `docs/PLAY_STORE.md` for
the `MANAGE_EXTERNAL_STORAGE` declaration and the Data safety answers.

## Known gaps and decisions to confirm

- **Not yet verified on a device.** CI builds every module, passes lint, runs the JVM unit
  tests and produces an R8 release APK. The instrumented tests (Room DAOs, Compose UI),
  the macrobenchmarks and the Phase 1 acceptance checks in `docs/PLAY_STORE.md` still need a
  device or emulator.
- Library versions in `gradle/libs.versions.toml` are pinned to a known-compatible set
  (Kotlin 2.2.21, AGP 8.13.0). Spec 3 asks for the latest stable; bump them in one change.
- `core:ui` is an extra module so the two feature modules can share the theme.
- The Baseline Profile in `app/src/main/baseline-prof.txt` is hand-written; replace it with a
  generated one (command above) before release.
- Open decisions from spec 19: final name and package (`com.quickfind.app` is a placeholder;
  update it in `app/build.gradle.kts` and `app/src/main/res/xml/shortcuts.xml`), default search
  engine and region.
