# HidR-A

A customizable Android **home-screen launcher** that lets you:

- **Rename and re-icon any app** as it appears in the launcher.
- **Hide apps** from the app grid while keeping them one tap away in a
  **PIN-protected Hidden list** inside the launcher.
- **Change the launcher's own name and icon** in the app drawer (e.g. make it
  show up as "Calculator" or "Notes").

## Honest scope — read this first

Android (without root) deliberately limits what one app can do to another:

- **Renaming / re-iconing another app affects this launcher's UI only.** The
  app's real name and icon everywhere else on the device are untouched. No app
  can rewrite another app's manifest.
- **Hiding removes an app from *this* launcher's grid only.** The app stays
  installed and remains visible to other launchers, in the Settings app, and in
  the system share/app pickers. There is no root-free way to hide an app from
  the whole OS, and HidR-A does not attempt one.
- **Changing HidR-A's own icon/name** is the one change that is truly
  system-wide, and it works because an app *is* allowed to re-brand itself, via
  manifest `activity-alias` entries toggled at runtime.

This is standard launcher / privacy-drawer behavior. It is meant for organizing
and customizing **your own** device.

## How each feature works

| Feature | Mechanism |
|---|---|
| List apps | `PackageManager.queryIntentActivities` for `MAIN`/`LAUNCHER` (a home app needs no `QUERY_ALL_PACKAGES`). |
| Rename / borrow icon | Per-app override stored locally; applied only when this launcher draws the grid. |
| Hide + vault | Override flag filters the app out of the grid; the Hidden screen is gated by a SHA-256 PIN. |
| Self icon/name | `DisguiseManager` enables one `activity-alias` and disables the others via `setComponentEnabledSetting`. |

## Build it yourself

Requirements: Android Studio (or the Android SDK + JDK 17).

```bash
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

Install with `adb install app/build/outputs/apk/debug/app-debug.apk`, then press
Home and choose HidR-A to try it as your launcher.

## Download a prebuilt APK

Every push runs the **Build APK** GitHub Actions workflow, which attaches
`hidr-a-debug-apk` as a downloadable build artifact. Push a tag like `v1.0` to
also publish the APK to a GitHub Release.

> The CI output is a **debug-signed** APK for testing. For distribution, add a
> release `signingConfig` and build `assembleRelease`.

## Project layout

```
app/src/main/java/com/hidra/launcher/
├── MainActivity.kt            # HOME activity; hosts Compose UI
├── data/
│   ├── Models.kt              # AppInfo, Override, Disguise
│   ├── PrefsRepository.kt     # overrides + PIN hash + disguise (SharedPreferences/JSON)
│   ├── AppRepository.kt       # enumerate launchable apps, apply overrides
│   └── DisguiseManager.kt     # toggle activity-alias for self re-branding
└── ui/
    ├── Theme.kt
    ├── MainViewModel.kt
    ├── LauncherRoot.kt        # scaffold + app grid
    ├── Sheets.kt              # customize sheet + icon picker
    └── Screens.kt             # hidden vault, settings, PIN
```
