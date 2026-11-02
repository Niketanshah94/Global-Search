# Play Store submission notes (Phase 1)

## Permissions declaration: `MANAGE_EXTERNAL_STORAGE`

Declare the core use case as **"On-device file search"** in Play Console → App content → All files access.

Suggested justification:

> QuickFind is a universal search app. When the user turns on "Full file search" it reads file
> and folder names across shared storage to build a search index that stays on the device, so the
> user can find documents, downloads and other non-media files by name. File contents are not read
> and nothing is uploaded. The permission is optional: without it QuickFind searches media files and
> any folders the user picks through the system folder picker.

The in-app explanation shown before the system screen is `disclosure_all_files_text` in
`feature/settings/src/main/res/values/strings.xml`. Expect a manual review.

## Prominent disclosure

Contacts and files each show an explanation dialog before the system permission prompt
(`SettingsScreen.kt` → `DisclosureDialog`; onboarding shows the same text inline).

## Data safety form

| Question | Answer for Phase 1 |
|---|---|
| Does the app collect or share user data? | No. Contacts, file names, app list and search history are processed on the device only. |
| Is data encrypted in transit? | Yes. The only network request is an HTTPS download of public exchange rates from api.frankfurter.app, made when the user types a currency conversion. It sends no user data and can be turned off in Settings. |
| Can users request deletion? | Yes, in-app: Settings → Privacy & data → Clear search history / Delete all index data. Uninstalling removes everything. |

Update the form when Phase 3 adds cloud AI and billing.

## Release checklist (spec 17, Phase 1 "Done when")

- [ ] "wh" shows WhatsApp as the top hit and Enter opens it (device with WhatsApp).
- [ ] Contacts searchable by name and partial number; Call, SMS and WhatsApp actions work.
- [ ] Files and folders searchable with media permission; full search works after All files access.
- [ ] Widget, Quick Settings tile and assistant entry open search with the keyboard up.
- [ ] Performance budgets (spec 9) met on the low-end test device: run `:benchmark` and record results here.
- [ ] Works with zero permissions granted.
- [ ] Play pre-launch report clean; Data safety form complete.

## Manual QA matrix (spec 16)

Pixel (latest Android) · Samsung One UI · Xiaomi HyperOS (check WorkManager jobs run) · low-end Android 8/9 ·
work profile · an RTL language · largest font size.
