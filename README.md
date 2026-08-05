# Swing Tracker

A local Android app that tracks your NSE stock watchlist and notifies you when a
stock matches a trend + relative-strength + volume screener suited to 3-5 month
swing/position trades. Everything runs on your phone - no server, no account,
no paid API.

**Not financial advice.** This screens for a commonly used technical setup
(trend confirmation + relative strength + volume). It is not a guarantee of
performance and carries real risk of loss like any trading strategy.

## The rule it checks (runs automatically every ~30 minutes)

1. Price > 50-day EMA, and 50-day EMA > 200-day EMA (confirmed uptrend)
2. Stock's 3-month return > Nifty 50's 3-month return (relative strength)
3. Today's volume > 20-day average volume (demand confirmation)

All three must be true for a stock to show as **MATCH** and trigger a notification.

Data comes from Yahoo Finance's public chart endpoint (free, no key) using
**daily** closing prices - this is not an intraday/live-tick tracker.

---

## Building from your phone only (no computer)

If you don't have a computer, skip the Android Studio steps above entirely -
GitHub will build the app for you in the cloud, and you just download the
finished file to your phone. See the separate walkthrough I'll give you in
chat for the exact steps (Termux + GitHub).

---

## Step 1: Install Android Studio

1. Download from https://developer.android.com/studio (pick the version for your OS).
2. Run the installer, accept the defaults ("Standard" install type is fine).
3. On first launch it downloads the Android SDK - this can take 10-20 minutes. Let it finish.

## Step 2: Open the project

1. Unzip the file I gave you anywhere on your computer.
2. Open Android Studio → **File > Open** → select the unzipped `SwingTracker` folder.
3. Android Studio will start a **Gradle Sync** automatically (bottom status bar).
   This project doesn't include the Gradle wrapper jar (a binary file); if
   Android Studio prompts *"Gradle wrapper is missing, use the bundled one?"*
   or similar, click yes/OK. First sync also takes a while - it's downloading
   the libraries listed in `app/build.gradle.kts`.
4. If sync fails with a version-related error, click the "Update" or "Install
   missing SDK" links Android Studio shows inline - it's usually one click.

## Step 3: Run it on your phone

1. On your phone: **Settings > About phone** → tap "Build number" 7 times to
   unlock Developer Options.
2. **Settings > Developer Options** → enable **USB debugging**.
3. Plug your phone into your computer via USB. Accept the "Allow USB
   debugging?" prompt on the phone.
4. In Android Studio, your phone should appear in the device dropdown (top
   toolbar, next to the green Run ▶ button). Select it and click Run.
5. The app installs and opens on your phone directly - no Play Store needed.

You can then unplug your phone; the app and its background checks keep
running on their own.

## Using the app

- Tap **+** to add a stock by its NSE symbol (just the ticker, e.g. `RELIANCE`,
  `TCS`, `INFY` - don't add `.NS`, the app does that internally).
- Each row shows price, 50/200 EMA, 3-month return vs Nifty, and a **MATCH** /
  **watching** badge.
- Tap the refresh icon (top right) to force an immediate check instead of
  waiting for the next 30-minute cycle.
- You'll get a phone notification the moment a stock newly qualifies (it won't
  spam you repeatedly while it stays qualifying - only on that first match).

## Known limitations (worth knowing upfront)

- **Not real-time.** Uses daily closing data, refreshed roughly every 30 min while
  the app has run at least once - Android may delay this further to save battery,
  especially if the app sits unused for days.
- **Yahoo Finance's endpoint is unofficial.** It's free and generally reliable,
  but Yahoo could change or rate-limit it without notice. If checks start
  failing (you'll see "Error: ..." on a stock row), that's the first place to look.
- **Minimum 30-min interval** is an Android WorkManager platform limit for
  periodic background work, not something we can tune much lower reliably.

## If something breaks

Come back and tell me the exact error message from Android Studio's "Build"
or "Logcat" tab - that's usually enough for me to fix it directly in the code.
