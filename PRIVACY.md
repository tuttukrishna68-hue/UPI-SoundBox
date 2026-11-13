# Privacy Policy

**UPI Soundbox** is designed with a simple privacy principle: **your data never leaves your phone**.

## What Data Is Accessed

- **Notification text** from UPI apps (Google Pay, PhonePe, Paytm, BHIM, Amazon Pay, CRED, and others) — used to detect and announce incoming payments
- **SMS messages** (optional) — only if you enable SMS-based parsing for UPI apps that send payment confirmations via SMS

The app reads only the text content of notifications from known UPI app package names. It does not read notifications from other apps.

## What Is Stored

When an incoming UPI payment is detected, the following is extracted and stored **locally on your device**:

- Payment amount
- Payer name or UPI VPA (as shown in the notification)
- Timestamp of the payment
- UPI reference number (if available)
- Source UPI app name

All stored data is **encrypted on device** using SQLCipher, with encryption keys managed by the Android Keystore system.

## What NEVER Leaves Your Phone

**Your payment and personal data.** UPI Soundbox has network access only when you explicitly tap **Check for updates**. That request reads a public version manifest from the project repository; payment data is never included, uploaded or transmitted. Specifically:

- ❌ No payment data is transmitted to any server
- ❌ No data is shared with any third party
- ❌ No data is uploaded to any cloud service
- ❌ No crash reports are sent anywhere

The update check is manual and optional. Payment monitoring, parsing, storage and
on-device TTS continue to work without network access.

## What Is NOT Collected

- ❌ No analytics or usage tracking
- ❌ No device identifiers (IMEI, Android ID, advertising ID)
- ❌ No location data
- ❌ No contacts or call logs
- ❌ No browsing history
- ❌ No camera or microphone recordings
- ❌ No biometric data (biometric auth is processed entirely by Android OS)

## Data Retention

- All data is stored **only on your device**
- **You control deletion**: clear transaction history anytime from the app settings
- Uninstalling the app removes all stored data
- No backups are sent to any external service

## Permissions Explained

| Permission | Why It's Needed |
|---|---|
| **Notification Access** | Required to read UPI payment notifications and announce them aloud |
| **SMS (optional)** | Optional — only if you enable SMS-based payment detection for certain UPI apps |
| **Battery Optimisation Exemption** | Requested so the announcement service keeps running reliably in the background |
| **Boot Completed** | To restart the notification listener after device reboot |
| **Foreground Service** | To keep the TTS announcement service running while the app is in the background |
| **Internet** | Only for the explicit GitHub update check; never for payment data |

## Open Source

UPI Soundbox is open source. You can audit the entire codebase to verify these privacy claims. The update checker is limited to the public manifest URL in `UpdateChecker.kt` and is never run in the background.

## Changes to This Policy

Any changes to this privacy policy will be documented in the [CHANGELOG](CHANGELOG.md) and reflected in a new app release.

---

*Last updated: 2024-12-01*

