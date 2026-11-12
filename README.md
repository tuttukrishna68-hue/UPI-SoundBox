# UPI Soundbox

> Hear every UPI payment instantly — like a hardware soundbox, on your phone.

UPI Soundbox announces incoming UPI payments aloud within about one second. It works with all major UPI apps, keeps a clean ledger of your daily collection, and never sends any data off your device.

## Features

- 🔊 Instant voice announcements for incoming UPI payments
- 📱 Works with Google Pay, PhonePe, Paytm, BHIM, Amazon Pay, CRED and more
- 🌐 10 Indian languages with customisable phrases
- 📊 Daily/weekly/monthly reports with app-wise breakdown
- 🔒 Fully offline — no internet permission, all data stays on device
- 📋 QR code generation for your UPI ID
- 🔋 Survives screen off, reboot, and battery optimisation

## Install

1. Download the latest APK from the [latest release](https://github.com/naukainsiders/UPI-SoundBox/releases/latest)
2. On your phone, allow installation from unknown sources
3. If the app is sideloaded, you may need to allow restricted settings (the app will guide you)
4. Grant notification access when prompted
5. Send a test payment and hear it announced!

## Updates

The `main` branch is the source of truth for the app. Every push runs the Android
build and unit tests in GitHub Actions and publishes a downloadable APK artifact.
Stable APKs are attached to [GitHub Releases](https://github.com/naukainsiders/UPI-SoundBox/releases).

To get the newest development build, open the latest successful workflow run under
[Actions](https://github.com/naukainsiders/UPI-SoundBox/actions) and download its `upi-soundbox-debug-apk` artifact.

## Build from Source

```bash
# Clone
git clone https://github.com/naukainsiders/UPI-SoundBox.git && cd UPI-SoundBox

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest

# Build signed release (set env vars first)
export KEYSTORE_PATH=path/to/keystore.jks
export KEYSTORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...
./gradlew assembleRelease
```

## Architecture

Multi-module Kotlin app: `app`, `core:parser`, `core:data`, `core:tts`, `core:design`, `feature:*`.
Jetpack Compose, MVVM, Hilt, Room, WorkManager, Coroutines/Flow.

## Privacy

- No INTERNET permission — enforced by CI
- All data encrypted on device (SQLCipher + Android Keystore)
- No analytics, ads, or crash reporting
- See [PRIVACY.md](PRIVACY.md) for full details

## Contributing

Issues and PRs welcome. For parser improvements, please use the **Parser Request** issue template with a redacted notification sample.

## License

TBD — see [LICENSE](LICENSE)
