# UPI Soundbox

> Hear every UPI payment instantly — like a hardware soundbox, on your phone.

UPI Soundbox announces incoming UPI payments aloud within about one second. It works with all major UPI apps, keeps a clean ledger of your daily collection, and never sends any data off your device.

## Features

- 🔊 Instant voice announcements for incoming UPI payments
- 📱 Works with Google Pay, PhonePe, Paytm, BHIM, Amazon Pay, CRED and more
- 🌐 10 Indian languages with customisable phrases
- 📊 Daily/weekly/monthly reports with app-wise breakdown
- 🔒 Offline-first payments — no payment data leaves the device
- 📋 QR code generation for your UPI ID
- 🔋 Survives screen off, reboot, and battery optimisation

## Install

1. Download the latest debug APK from the [latest successful Actions run](https://github.com/tuttukrishna68-hue/UPI-SoundBox/actions)
2. On your phone, allow installation from unknown sources
3. If the app is sideloaded, you may need to allow restricted settings (the app will guide you)
4. Grant notification access when prompted
5. Send a test payment and hear it announced!

## Updates

The `main` branch is the source of truth for the app. The Settings screen checks
the version manifest in this repository when you explicitly tap **Check for updates**.
If a newer version is available, it provides a direct APK download link.

Payment monitoring, parsing, storage and TTS remain fully on-device. Network access
is used only for the manual update check and never for payment data.

## Build from Source

```bash
# Clone
git clone https://github.com/tuttukrishna68-hue/UPI-SoundBox.git && cd UPI-SoundBox

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

- INTERNET permission is used only for the explicit GitHub update check; payment data is never uploaded
- All data encrypted on device (SQLCipher + Android Keystore)
- No analytics, ads, or crash reporting
- See [PRIVACY.md](PRIVACY.md) for full details

## Contributing

Issues and PRs welcome. For parser improvements, please use the **Parser Request** issue template with a redacted notification sample.

## License

TBD — see [LICENSE](LICENSE)

