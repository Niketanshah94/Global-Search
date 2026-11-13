# FindPal Pro: Google Play subscription setup

The app already contains the billing code. To start selling, set up the items below in **Play Console**. The IDs must match exactly, because the app looks them up by name.

## What users get

| | FindPal Free | FindPal Pro |
|---|---|---|
| Search contacts (including Hindi and other Indian-script names) | ✓ | ✓ |
| Call a contact or a typed number, open the contact card | ✓ | ✓ |
| Apps, files, folders, settings, calendar, notifications | – | ✓ |
| WhatsApp, SMS, Telegram, Signal, email and maps from a contact | – | ✓ |
| OTP copy, maths, currency, units, timers and alarms | – | ✓ |
| Search by meaning, pins, nicknames, recent apps and files | – | ✓ |

When a free user searches, they see their contacts plus a card saying "Pal found N more results". Tapping a locked button, such as WhatsApp, opens the paywall with a headline about that feature. The paywall also appears once at the end of onboarding, where users can choose "Continue with Free". The rules for what is free live in `core/model/.../ProGate.kt`.

## 1. Payments profile
Go to Play Console → **Settings → Payments profile** and create the merchant account. You'll need your legal name, address, bank account and tax details. Subscriptions can't be created until this is done.

## 2. The subscription
Go to **Monetize with Play → Products → Subscriptions → Create subscription**.

- **Product ID:** `findpal_pro`. This can never be changed later.
- **Name:** FindPal Pro
- **Benefits** (shown in Play's subscription centre): "Search apps, files and settings", "WhatsApp & SMS from search", "OTP copy and quick answers", "Search by meaning".

### Base plans
Add two base plans, both **Auto-renewing**:

| Base plan ID | Billing period | Price (USD) | Notes |
|---|---|---|---|
| `monthly` | 1 month | **$4.99** | |
| `yearly` | 1 year | e.g. **$39.99** | Shown first on the paywall with "Save 33%", which the app calculates from the two prices. |

Set the USD price and let Play convert it for other countries. You can override individual countries if you want, for example India at ₹149/month and ₹1,199/year. The app always shows whatever price Play sends it, so a price change needs no app update.

Recommended settings: grace period **7 days**, account hold **30 days**. Changing plans is optional.

### Free trial offer (one on each base plan)
For each base plan, choose **Add offer**:

- **Offer ID:** `free-trial`
- **Eligibility:** *New customer acquisition → Never had this subscription*
- **Phase:** *Free trial*, **7 days**

Play only shows the trial to people who are eligible, so each Google account gets one trial. People who aren't eligible see the normal price and a "Subscribe" button. The app handles both cases.

Activate both base plans and both offers.

## 3. Purchase signature check (recommended)
Go to **Monetize with Play → Monetization setup** and copy the **Licensing** base64 public key.
Add it as a GitHub Actions secret named `PLAY_LICENSE_KEY` (repo **Settings → Secrets and variables → Actions**). The CI build already reads it.

With the key in place, the app checks Google's signature on every purchase, so a faked purchase can't unlock Pro. Without it, the app trusts whatever Google Play reports.

## 4. Testing
- **Setup → License testing:** add your Gmail. Test purchases are free, and the 7-day trial renews every few minutes so you can watch renewals and cancellations.
- Upload a signed release build to **Internal testing**. Billing only works for an app installed from Play, not for a debug APK installed directly from a file.
- Test builds (debug APKs) have a **Settings → "Test: unlock Pro"** switch, and an "Unlock Pro (test build only)" button on the paywall. Use them to try Pro without Play. Neither appears in release builds.

## 5. Store listing and policy
- **App pricing:** keep the app *Free*. Pro is an in-app subscription.
- **Privacy policy URL:** required. Say that payments are processed by Google Play and that FindPal never receives card details.
- **Data safety:** purchases are handled by Google Play, and FindPal itself collects no data. The only network request the app makes is for public currency rates.
- The paywall already shows the price after the trial, says the subscription renews automatically, and explains how to cancel, as Play's subscription policy requires.

## How the app tracks the subscription
- `core/billing/BillingRepository.kt` uses Play Billing Library 8 and needs no server.
  - It connects when the app starts, and re-checks the subscription each time the app comes to the foreground.
  - It confirms new purchases with Google (Play refunds any it hasn't confirmed within 3 days), retrying until that succeeds.
  - It saves "is Pro" on the phone (`EntitlementRepository`), so Pro works offline.
- Play's purchase list only includes subscriptions that are currently active, including trials and the grace period. When the trial ends without payment, or a subscription expires or goes on hold, the next check turns Pro off. Search then drops back to contacts only.
- Users manage or cancel the subscription from **Settings → FindPal Pro**, which opens Google Play's subscriptions page.
