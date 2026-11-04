# Changelog

All notable changes to QuickFind are recorded here. Format: [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased] — Phase 1 MVP

### Changed — brand
- Renamed to **FindPal**: app name, widget, settings, intro and privacy texts; Play Store ID `com.findpal.app` (installs alongside older test builds, which can be uninstalled). Store listing text in `docs/store/`, brand assets in `docs/brand/`.

### Changed — "Thumb Zone" design
- New warm palette (paper background, white cards, ink text, orange accent) with a matching warm dark mode; replaces the violet Aurora look across search, intro, widget and app icon.
- Search bar is now at the bottom by default, above the keyboard; results stack upward so the top hit is right under the thumb (top position is still available in Settings).
- Top hit is a dark card with large labelled action tiles (Call, SMS, WhatsApp…); WhatsApp in green.
- Filter chips above the bar ("All · Contacts 3 · Apps 1 · Files 2 · Web"); a chip shows every result of that kind.
- Each result is its own white card with "Type · detail" subtitles; the bar shows what Enter will do ("↵ Call").
- Empty screen: OTP and paste pills, "Try" suggestion chips and a recent-apps card, all anchored above the bar.

### Changed — intro screens
- Emoji replaced with vector icons: gradient-filled hero orbs (search, shield, key, sparkle), white icons on glowing gradient tiles for every privacy point, permission and entry point, and a check badge for allowed permissions. The OTP and paste chips and the "Top hit" label use icons too.
- Permissions are now asked during the intro: a "Your phone. Your data." screen explains privacy in four short points, then an "Allow access" screen gives each permission (contacts, files, calendar, notifications) a one-line reason and an Allow button, with "Allow & continue" to grant all at once and "Not now" to skip. Settings → About → Show intro again replays it.
- Aurora-style onboarding: drifting aurora background, a breathing glowing search orb, a demo search bar that types example queries and shows answers, gradient headlines, animated page dots, glass source cards with toggles (apps, contacts, files, calendar), entry-point cards and a glowing "Start searching" button. Motion stops with "Remove animations".

### Added — Sprint B
- Indian-language names from English typing: Devanagari, Gujarati, Bengali, Gurmukhi, Odia, Tamil, Telugu, Kannada and Malayalam names are also matched in Latin letters ("rahul" finds "राहुल", "niketan" finds "નિકેતન"), for apps, contacts and files.
- Notification search (last 7 days, on device) and a "Copy OTP" chip for codes from the last 10 minutes. Uses a notification listener that only stores while the source is on; Settings → Privacy can clear the history.
- Calendar search, 30 days back to 30 days ahead, upcoming events first, with Navigate for event locations.
- India-aware app keywords: "payment"/"upi" find PhonePe, GPay, Paytm and BHIM; "taxi", "food", "grocery", "train", "stocks", "medicine" and more; Android app categories ("games", "music", "photos").
- Search by meaning (beta): a 6 MB on-device embedding model (Universal Sentence Encoder via MediaPipe Text Embedder, Apache 2.0) finds apps by what they do. Runs after the 80 ms pause and scores below name matches.

### Added — Sprint A
- Unit converter (length, weight, temperature, data, volume, speed), offline: "5 ft in cm", "30 c to f", "10 km".
- Currency converter: "100 usd in inr", "$50", "€20 to inr", using European Central Bank rates from frankfurter.app, cached 12 hours. Adds the `INTERNET` permission; nothing typed is sent, and it can be turned off in Settings.
- Timers and alarms from text: "timer 10 min", "timer 1h 30m", "alarm 6:30", "wake me at 7am" (opens the Clock app pre-filled).
- Search shortcuts: "f ", "c ", "a ", "s " restrict to files, contacts, apps or settings (shown as a chip); "yt ", "g ", "ddg ", "bing ", "pplx ", "play " search that site.
- Nicknames: long-press a result → Nickname; typing it later puts that result first.
- Contacts: Telegram and Signal buttons when installed, and Navigate for saved addresses.
- Smart paste: email addresses offer "Email", postal addresses offer "Open in Maps".

### Fixed
- The cursor jumped back one character while typing, and typing lagged by a frame (the field now reads its text from snapshot state).

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
