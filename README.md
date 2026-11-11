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

1. Download the latest APK from [Releases](../../releases)
2. On your phone, allow installation from unknown sources
3. If the app is sideloaded, you may need to allow restricted settings (the app will guide you)
4. Grant notification access when prompted
5. Send a test payment and hear it announced!

## Build from Source

```bash
# Clone
git clone <repo-url> && cd SoundBox

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
