# Publishing FindPal on Google Play: every field, in order

Work through the steps in this order. Text to paste is in **bold** or in code blocks. Where you have to decide something yourself, it's marked **⚠ You decide**.

Files used below:
- `docs/store/LISTING.md`: name, descriptions, category
- `docs/store/images/`: icon, feature graphic, 7 screenshots
- `docs/store/BILLING.md`: subscription setup (product IDs and prices)
- `website/privacy.html`: privacy policy page. Host it and use its URL.

---

## 0. Developer account (one time, play.google.com/console)

| Field | What to enter |
|---|---|
| Account type | **⚠ You decide.** **Organization** if you publish under a company. That needs a free D-U-N-S number (apply at dnb.com, takes about 1–2 weeks), and the store shows your company name. **Personal** is quicker to open, but new personal accounts must run a **closed test with at least 12 testers for 14 days in a row** before production is unlocked. |
| Registration fee | US$25, one time |
| Developer name (shown on the store) | Your company or brand name. "FindPal" is fine if no one else uses it. |
| Contact email / phone / website | Shown publicly on the store page. Use a support address, not a personal one. |
| Identity verification | Government ID (and company documents for an organization). This can take a few days. |

Before going further, put the same developer name and support email into `website/privacy.html` (replace `DEVELOPER_NAME` and `SUPPORT_EMAIL`). Then deploy the website so that `https://<your-domain>/privacy.html` works.

---

## 1. Create app (Home → Create app)

| Field | Value |
|---|---|
| App name | **FindPal: Search Your Phone** |
| Default language | **English (United States) – en-US** |
| App or game | **App** |
| Free or paid | **Free** (Pro is an in-app subscription. A free app can never later become a paid download, and that's what we want.) |
| Declarations | Tick **Developer Program Policies** and **US export laws** |

The package name is set by the first upload and is **`com.findpal.app`**. It can never be changed afterwards.

---

## 2. Set up your app → App content (Policy and programs → App content)

### Privacy policy
`https://<your-domain>/privacy.html`

### App access
Choose **All or some functionality in my app is restricted**, then add one set of instructions:
- Name: **FindPal Pro features**
- Instructions:
```
No account or login. FindPal Free (contact search and calling) works immediately after install.
Pro features (apps, files, settings, OTP copy, quick answers, WhatsApp/SMS actions) are unlocked
with the FindPal Pro subscription, which includes a 7-day free trial: open the app, tap "Go Pro"
(top right) or any locked result, and start the free trial.
```

### Ads
**No, my app does not contain ads.**

### Content rating (questionnaire)
- Email: your support email
- Category: **All Other App Types**
- Answer **No** to everything about violence, sexuality, bad language, controlled substances, crude humour and gambling.
- Users can interact or exchange content with each other: **No**. FindPal opens WhatsApp and similar apps, but there's no chat inside FindPal.
- Shares the user's physical location with others: **No**
- Lets users buy digital goods: **Yes**
- Web browser or search engine with unrestricted web access: **No**. FindPal searches the phone; a web search is handed off to the browser.
- Expected rating: **Everyone / PEGI 3 / IARC 3+**

### Target audience and content
- Target age groups: **18 and over**. This is the simplest choice for review. You can add 13–15 and 16–17, but **never under 13**: that brings in the Families policy.
- Could the app unintentionally appeal to children? **No**

### News app
**No**

### Data safety
FindPal processes data **only on the device**. Google says on-device-only processing does **not** count as "collected".

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| (If shown) Is all user data encrypted in transit? | **Yes** (HTTPS only) |
| Do you provide a way for users to request that their data is deleted? | **Yes**: in-app *Settings → Privacy → clear history and index*, or uninstall |

Why "No" is correct:
- Contacts, files, calendar, notifications and history are read and indexed on the phone and never sent anywhere.
- The only network request fetches public currency rates (frankfurter.app) and contains no user data.
- Payments are processed by Google Play, not by FindPal.
- There are no analytics, crash-reporting or ads SDKs.

### Advertising ID
**No**. The app doesn't use the advertising ID.

### Government app / Financial features / Health
- Government app: **No**
- Financial features: **My app doesn't provide any financial features**. Copying an OTP isn't a financial service.
- Health: **My app does not have any health features**

### Sensitive permissions (Play asks once the bundle is uploaded) ⚠ main review risk

| Permission | Form to fill | What to say |
|---|---|---|
| `MANAGE_EXTERNAL_STORAGE` (All files access) | **All files access** declaration | Core use: **On-device file search** (a permitted use). "FindPal is a universal search app. All files access lets users find documents, downloads and other non-media files by name anywhere on their phone. Only file and folder names are indexed, on the device; contents are never read or uploaded. The permission is optional and requested only when the user turns on full file search." You'll also need a **YouTube video link** (unlisted is fine, about 30 s) showing: turning on file search → the explanation screen → Android's All-files screen → searching a PDF by name. |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` | **Photo and video permissions** declaration | Core use: "Search: users find photos and videos by name from the search bar, alongside apps, contacts and files. The app indexes media names on the device." |
| `READ_CONTACTS`, `READ_CALENDAR`, `CALL_PHONE`, notification access | No form | The in-app explanation screens shown before each request already meet the "prominent disclosure" rule. |

If Google rejects the All files access or photo/video declarations, tell me. I can ship a version without them: file search then works through Android's folder picker and media index, with no special permission.

---

## 3. Store settings (Grow → Store presence → Store settings)

| Field | Value |
|---|---|
| App category | **Tools** (Productivity also fits; Tools has less competition) |
| Tags | Up to 5 from Google's list. Pick: **Search**, **Productivity**, **Utilities**, **Launcher**, **Personalization** (whichever exist in the picker) |
| Email | Your support email (**required**) |
| Phone | Optional |
| Website | `https://<your-domain>/` |
| External marketing | Leave on |

---

## 4. Main store listing (Grow → Store presence → Main store listing)

| Field | Value |
|---|---|
| App name (30) | **FindPal: Search Your Phone** |
| Short description (80) | **Find apps, contacts, files, OTPs & settings in 2 letters. Private, on-device.** |
| Full description (4,000) | Copy the full description from `docs/store/LISTING.md` (2,785 characters, including the Free/Pro section) |
| App icon | `docs/store/images/icon-512.png` (512×512) |
| Feature graphic | `docs/store/images/feature-graphic.png` (1024×500) |
| Phone screenshots | `docs/store/images/screenshot-1.png` … `screenshot-7.png` (1080×1920). Upload them in that order. |
| 7" and 10" tablet screenshots | Optional. Skip for now. |
| Video | Optional YouTube link. The All-files demo video can double as this. |

Play requires screenshots to show the real app. These are accurate renders of the current UI. Once you have the app on a phone, replace any that differ with real captures.

**Extra languages (optional, later):** Hindi (hi-IN) is worth adding for India. I can translate the listing.

---

## 5. Monetise → subscriptions

Follow `docs/store/BILLING.md`: payments profile → subscription `findpal_pro` → base plans `monthly` ($4.99) and `yearly` (**⚠ You decide**; I suggest $39.99) → a `free-trial` offer of 7 days on each → activate. You can only create these after the first bundle has been uploaded (step 7), so it's fine to come back to this step.

---

## 6. Pricing and distribution (Monetise → App pricing / Reach → Countries)

| Field | Value |
|---|---|
| Price | **Free** |
| Contains ads | **No** |
| In-app purchases | **Yes** (Play sets this once the subscription exists) |
| Countries | **⚠ You decide.** All countries, or start with **India** and add others later. Pro prices are converted to local currency automatically. |
| Devices | Phones (default). Leave tablets and Chromebooks on; the app works on them. |

---

## 7. The app bundle and your first release

### 7a. Create your upload key (one time, on your computer)
```bash
keytool -genkeypair -v -keystore findpal-upload.jks -alias findpal \
  -keyalg RSA -keysize 2048 -validity 10000
```
Keep `findpal-upload.jks` and its passwords somewhere safe, such as a password manager. Never commit them. Play App Signing (on by default) holds the real signing key; this upload key only proves uploads come from you, and Google can reset it if you lose it.

### 7b. Give it to GitHub so CI builds the signed bundle
On GitHub, go to repo **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret | Value |
|---|---|
| `UPLOAD_KEYSTORE_BASE64` | Output of `base64 -w0 findpal-upload.jks` (macOS: `base64 -i findpal-upload.jks`) |
| `UPLOAD_STORE_PASSWORD` | The keystore password |
| `UPLOAD_KEY_ALIAS` | `findpal` |
| `UPLOAD_KEY_PASSWORD` | The key password (often the same) |
| `PLAY_LICENSE_KEY` | From Play Console → Monetise → Monetisation setup → Licensing (add once the app exists) |

Each push then also produces a **`findpal-play-bundle`** artifact (`app-release.aab`) on the Actions run. Its version code is the CI run number, so it goes up with every build automatically.

### 7c. Internal testing first (Test and release → Testing → Internal testing)
1. **Create new release** → accept **Play App Signing** → upload `app-release.aab`.
2. Release name: **1.0.0**
3. Release notes (en-US):
```
<en-US>
Meet FindPal! One search bar for your whole phone.
• Find apps, contacts, files, settings and OTPs in two letters
• Contacts saved in Hindi and 8 more Indian scripts match English typing
• Maths, currency, units, timers and alarms right in search
• Private by design: everything is searched on your phone
Try FindPal Pro free for 7 days.
</en-US>
```
4. **Testers:** create an email list with your own Gmail, plus up to 100 others. Open the opt-in link on your phone and install from Play. Google Play billing only works for installs from Play.
5. Add the same Gmail under **Setup → License testing**, so test subscriptions are free and the trial renews within minutes.

### 7d. Closed test (personal accounts only: 12+ testers for 14 days in a row)
**Testing → Closed testing → Create track** → upload the same bundle → add at least 12 testers' emails (friends, family, a Google Group) → each one must opt in and keep the app installed for 14 days. Then go to **Dashboard → Apply for production** and answer the short questionnaire about the test.

### 7e. Production (Test and release → Production)
**Create new release** → promote the tested bundle → choose countries → **Send for review**. The first review usually takes **1–7 days**; it can be longer if the permission declarations need a closer look.

---

## Checklist of things only you can do
- [ ] Choose a developer account type, pay the $25 fee and verify your identity
- [ ] Support email + developer name → `website/privacy.html` → deploy the website
- [ ] Pick the yearly price and which countries to launch in
- [ ] Create the upload key and add the 4 GitHub secrets (then 5 with `PLAY_LICENSE_KEY`)
- [ ] Record the 30-second All-files-access demo video (unlisted YouTube)
- [ ] Payments profile, then the subscription (BILLING.md)
- [ ] Recruit 12 closed testers (personal accounts only)
