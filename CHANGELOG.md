# Changelog

All notable changes to UPI Soundbox will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [1.0.0] - 2026-11-12

### Added
- **Multi-Module Architecture**:
  - Pure Kotlin module `:core:parser` with zero Android framework dependencies.
  - Core modules `:core:data` (Room + SQLCipher), `:core:tts` (Text-to-Speech + Audio Focus), `:core:design` (Material 3 editorial theme).
  - Feature modules: `:feature:onboarding`, `:feature:home`, `:feature:counter`, `:feature:history`, `:feature:reports`, `:feature:myqr`, `:feature:settings`, `:feature:diagnostics`.
- **UPI Parser Engine**:
  - Support for Google Pay, Google Pay Business, PhonePe, PhonePe Business, Paytm, Paytm Business, BHIM UPI, Amazon Pay, CRED, and generic fallbacks.
  - Strict credit keyword detection and noise/debit rejection.
  - Indian currency formatting (paise representation as Long, ₹/Rs./INR symbol matching, Indian digit grouping).
  - Payer name extraction and VPA masking (`ab****e@bank`).
  - UPI reference / UTR / RRN extraction.
  - Cross-source 90-second deduplication engine.
  - 121 table-driven JSON fixtures with automated test runner.
- **Indian Number-to-Words**:
  - Native word conversion with lakh/crore/paise units for English (India), Hindi, Malayalam, Tamil, Telugu, and Kannada.
- **Announcement Engine & Background Services**:
  - `SoundboxService` foreground service with `specialUse` foreground service type and persistent summary notification.
  - Audio focus management (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) with automatic volume boost and restore.
  - Synthesized dual-tone PCM chime chord without external media dependencies.
  - Burst payment handling (>3 payments in 10s summarized into single count and total).
  - Quiet hours and alarm stream override mode.
  - Phone call detection to pause announcements while user is speaking on a call.
- **Data & Security**:
  - SQLCipher 4.6.1 encrypted Room database with Android Keystore 256-bit AES master key.
  - Zero-internet guarantee: strictly no `android.permission.INTERNET` requested.
  - Biometric / PIN authentication lock for app access.
  - Screenshot protection (`FLAG_SECURE`).
- **UI & Design System**:
  - Channel D inspired editorial styling: Paper (#FAFAF8), Ink (#141414), Accent Orange (#F25C2E).
  - Bundled offline typefaces: Merriweather display serif and Plus Jakarta Sans body.
  - Original vector illustrations: shopkeeper with phone, notification rupee coin, speaker check, empty mailbox, permission shield.
  - 13 complete application screens: Splash, 3-slide Onboarding, Setup Wizard, Home with Live Chat Feed & pulsing listening ring, Counter Mode, History with CSV export, Reports with custom Canvas charts, My QR with PDF Standee export, Voice Settings, UPI Apps, Help conversational UI, Settings, and Diagnostics with 5-minute learning mode and redacted fixture export.
- **Reliability & OS Integration**:
  - Android 13+ restricted settings illustrated guidance flow.
  - WorkManager 15-minute background watchdog with `requestRebind()` auto-recovery.
  - Direct Boot and post-reboot autostart handler.
  - OEM battery and autostart optimization deep-link guide for Xiaomi, Samsung, Oppo, Vivo, OnePlus, and Pixel.
- **CI/CD**:
  - GitHub Actions CI workflow with automated test runs, linting, and zero-internet permission grep enforcement.
  - Automated GitHub Releases APK signing workflow.
