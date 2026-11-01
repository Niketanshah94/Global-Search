# Changelog

All notable changes to QuickFind are recorded here. Format: [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased] — Phase 1 MVP

### Changed — "Aurora" design
- New look for the search overlay: drifting aurora gradient behind a frosted-glass panel, a search pill with an animated gradient ring that spins faster while results load, a gradient greeting with one-tap suggestion chips, results grouped in glass cards with item animations, a glowing top-hit card and gradient icon tiles.
- Violet/magenta/cyan brand palette for light and dark; wallpaper colours (dynamic color) are now off by default and can be turned back on in Settings.
- Gradient home-screen widget and app icon.
- Contacts show one-tap Call, Message and WhatsApp icon buttons on every row; the top hit shows an icon row with short captions; the long-press sheet shows icons.
- Fixed: the list now jumps back to the top hit when results change (it used to stay scrolled), and text on the glass panels is no longer black in dark mode.
- All motion stops when the system "Remove animations" setting is on.

### Added
- Multi-module project (`app`, `core/*`, `feature/*`, `benchmark`) with a version catalog, Hilt, Compose and GitHub Actions CI (Phase 0).
- `core:matching`: from-scratch text normalizer (NFD diacritic folding that keeps Devanagari marks, camelCase/separator/digit word boundaries, index map for highlighting), Sublime-style fuzzy matcher with typo tolerance, history/frecency ranking, result grouping, a shunting-yard calculator and phone/URL detectors. Table-driven unit tests for every case in spec 4.6.
- `core:data`: Room database (query history, launch events, hidden and pinned results, file index with FTS4) and DataStore settings.
- `core:sources`: apps from all profiles via `LauncherApps` with a disk snapshot and a two-level icon cache; contacts (name, nickname, email, partial number); files and folders; ~60 verified system-settings shortcuts; calculator, phone-number, URL and web-search instant results; the `SearchEngine` query pipeline (in-memory results first, indexed results merged after an 80 ms debounce).
- `core:indexing`: MediaStore incremental indexing (generation-based on API 30+), daily deletion check, Tier B full walk with `walkFileTree`, Storage Access Framework folders, derived folder entries, WorkManager scheduling and a MediaStore observer while search is visible.
- `feature:search`: translucent search overlay with auto-focused field, top-hit card (Enter opens it), grouped sections with "Show more", matched-letter highlighting, long-press action sheet (secondary actions, pin, hide), clipboard paste chip, swipe-down to close, haptic tick, in-app folder view.
- `feature:settings`: settings screen (sources, engine, priorities, bar position, theme, privacy controls, permission status, about/licenses) and three-screen skippable onboarding.
- Entry points: launcher icon, Glance home-screen widget, Quick Settings tile, optional persistent notification, default-assistant (`ASSIST`) entry and static "Search files"/"Search contacts" shortcuts.
- Index database and app snapshot excluded from Auto Backup; R8 enabled for release.
- Macrobenchmarks (cold/warm start, typing "whats", scrolling) and a Baseline Profile generator, plus a starter `baseline-prof.txt`.
