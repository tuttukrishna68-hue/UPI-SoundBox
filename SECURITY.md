# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| latest  | ✅ Yes             |
| older   | ❌ No              |

We only provide security fixes for the latest release. Please update to the most recent version before reporting.

## Reporting a Vulnerability

If you discover a security vulnerability in UPI Soundbox, **please do not open a public issue**. Instead, report it responsibly:

1. **Email**: Send a detailed report to the repository maintainer (see profile)
2. **Subject line**: `[SECURITY] UPI Soundbox — <brief summary>`
3. **Include**:
   - A description of the vulnerability
   - Steps to reproduce it
   - The potential impact
   - Suggested fix (if any)

We will acknowledge receipt within **48 hours** and aim to provide a fix within **7 days** for critical issues.

## Scope

The following are in scope for security reports:

- **Data leakage**: Any path where payment data, UPI IDs, or transaction history could leave the device
- **Permission escalation**: The app acquiring permissions beyond what is declared
- **Encrypted storage bypass**: Weaknesses in SQLCipher or Android Keystore integration
- **Notification data exposure**: Payment data being logged, cached, or accessible to other apps
- **Intent/broadcast vulnerabilities**: Exported components that could be exploited

The following are **out of scope**:

- Attacks requiring physical access to an unlocked device with USB debugging enabled
- Social engineering attacks
- Denial-of-service attacks on the device itself
- Issues in third-party dependencies with no demonstrated exploit in this app

## Security Design Principles

UPI Soundbox is designed with security as a core principle:

- **No INTERNET permission**: The app cannot make network requests — enforced at build time by CI
- **On-device encryption**: All payment data is encrypted using SQLCipher with keys managed by Android Keystore
- **No analytics or telemetry**: Zero data collection, zero tracking
- **Minimal permissions**: Only notification access (required) and optional SMS reading
- **No exported components**: All activities, services, and receivers are internal

## Disclosure Policy

- We practice **coordinated disclosure**: we will credit reporters (unless anonymity is requested) once a fix is released
- Critical vulnerabilities will be patched and a new release published as quickly as possible
- A security advisory will be published on the GitHub repository for any confirmed vulnerability
