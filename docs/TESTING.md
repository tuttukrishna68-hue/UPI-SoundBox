# UPI Soundbox — Real-Device Testing Guide & Verification Checklist

This document provides a comprehensive, field-ready test script for QA testing of the UPI Soundbox application on real hardware under actual shopkeeper operational conditions.

---

## 1. Prerequisites & Test Devices

### Recommended Test Matrix
- **Android Versions:** MinSdk (Android 8.0 / 8.1 Oreo), Android 10, Android 13/14 (with Restricted Settings), Android 15.
- **OEMs to Test:**
  - Xiaomi / Redmi / Poco (MIUI / HyperOS — aggressive autostart & battery management)
  - Samsung (One UI — deep sleeping apps)
  - BBK: Vivo / iQOO (Funtouch OS) & Oppo / Realme / OnePlus (ColorOS / OxygenOS)
  - Clean Android: Google Pixel or Motorola

---

## 2. Setup & Installation Verification

### Test 2.1: Sideload & Restricted Settings (Android 13+)
1. Sideload `app-debug.apk` or `app-release.apk` directly via USB / file manager (not Play Store).
2. Launch app; complete Onboarding slides.
3. In Setup Wizard, click **Enable Notification Access**.
4. **Expected:**
   - If Android 13/14 detects sideload, the OS toggle shows "Restricted setting".
   - App surfaces the illustrated 4-step modal:
     1. Open **App info** (Settings > Apps > UPI Soundbox).
     2. Tap the top-right **three-dot menu (⋮)**.
     3. Select **Allow restricted settings** and authenticate (PIN/fingerprint).
     4. Return to app and enable notification access.
5. Verify status chip turns green with checkmark.

### Test 2.2: Permissions & Battery Optimization
1. In Setup Wizard, grant **Post Notifications** (Android 13+).
2. Tap **Exempt Battery Optimization**.
   - Check system dialog appears: "Let app always run in background?" Tap **Allow**.
3. Tap **Autostart Settings** (on Xiaomi/Oppo/Vivo/Realme).
   - Verify OEM-specific deep link opens the OS Autostart / Background Run manager directly.
4. Verify all wizard chips show green **Ready**.
5. Tap **Send Test Payment**.
   - Confirm speech output: *"Test announcement: Received ₹100 on Google Pay"*.
   - Confirm test payment does **not** write to Room database / History / Today's Total.

---

## 3. Real Payment Detection & Voice Announcements

### Test 3.1: Live UPI Apps Payment Test
Prepare two devices: Device A (Merchant phone running Soundbox), Device B (Customer phone).
Perform genuine ₹1.00 or ₹10.00 transfers to Device A's merchant QR / VPA from:
- [ ] **Google Pay** (Personal)
- [ ] **Google Pay for Business**
- [ ] **PhonePe** (Personal)
- [ ] **PhonePe Business**
- [ ] **Paytm** (Personal)
- [ ] **Paytm for Business**
- [ ] **BHIM UPI**
- [ ] **Amazon Pay**
- [ ] **CRED**

**Verification Criteria:**
- **Latency:** Announcement starts within ~1.0 second of notification appearance in the shade.
- **Accuracy:** Spoken amount matches transfer exactly (e.g., ₹10 is spoken as *"Ten rupees"* in English or *"दस रुपये"* in Hindi).
- **Payer & App:** Correctly extracts sender name or masked VPA and source app name.
- **Feed & Counter:** Live feed on Home displays new chat bubble; Counter Mode updates in massive typography.

---

## 4. Background Reliability & OS Survival

### Test 4.1: Screen-Off Playback
1. Lock Device A (screen completely off / black).
2. Wait 30 seconds.
3. Send ₹5.00 payment from Device B.
4. **Expected:** Chime plays and TTS announcement speaks loud and clear through speaker without waking the screen.

### Test 4.2: Doze Mode Survival
1. Enable developer options; connect device to ADB.
2. Force Doze mode:
   ```bash
   adb shell dumpsys deviceidle force-idle
   ```
3. Send ₹15.00 payment.
4. **Expected:** NotificationListenerService receives notification immediately; audio focus is gained and payment is spoken.

### Test 4.3: Task-Switcher Swipe-Away
1. Open Recent Apps (Overview / Task Switcher) on Device A.
2. Swipe UPI Soundbox away to dismiss the task.
3. Check notification shade: Foreground notification *Today's Collection* remains persistent.
4. Send ₹20.00 payment.
5. **Expected:** Soundbox still announces payment without interruption.

### Test 4.4: Device Reboot Recovery
1. Restart Device A (`adb reboot`).
2. Unlock phone once (Direct Boot / user unlock).
3. Do **NOT** open UPI Soundbox.
4. Wait 15 seconds for `BootReceiver` / `WatchdogWorker` to run.
5. Check notification drawer: Persistent soundbox notification is restored.
6. Send ₹50.00 payment.
7. **Expected:** Payment is announced successfully.

### Test 4.5: WorkManager Watchdog Self-Healing
1. In Android App Settings, force stop the `SoundboxService` notification listener.
2. Run watchdog manually via adb or wait 15 minutes:
   ```bash
   adb shell cmd jobscheduler run -f com.upisoundbox.app <JOB_ID>
   ```
3. **Expected:** Watchdog detects listener is disconnected, invokes `requestRebind()`, or triggers high-priority alert notification: *"Soundbox is not listening — Tap to fix"*.

---

## 5. Audio, Bluetooth & Phone Call Handling

### Test 5.1: Volume Boost & Restore
1. Set media volume to 20% (quiet).
2. Enable **Boost Volume During Announcement** in Voice Settings.
3. Trigger a payment.
4. **Expected:** Volume rises to configured announcement level (e.g. 100%) during speech and immediately restores to 20% afterwards.

### Test 5.2: Bluetooth Speaker Routing
1. Pair Device A with an external Bluetooth speaker (or Bluetooth neckband/earphones).
2. Toggle **Route to Bluetooth Speaker** in Voice Settings.
3. Send payment.
4. **Expected:** Chime and speech route clearly through external Bluetooth device.

### Test 5.3: Silent Mode / Alarm Stream Mode
1. Put Device A into **Silent / Do Not Disturb (DND)** mode (media stream muted).
2. Enable **Use Alarm Stream** in Voice Settings.
3. Send payment.
4. **Expected:** Announcement is audible through the alarm audio stream despite phone being on silent.

### Test 5.4: Incoming Phone Call Pausing
1. Place a regular phone call to Device A and answer it.
2. While on the call, send a UPI payment to Device A.
3. **Expected:** Announcement engine detects active phone call (`AudioManager.MODE_IN_CALL` or loss of audio focus) and queues the announcement until the call hangs up.

---

## 6. Burst Payment Handling

### Test 6.1: High-Velocity Rapid Burst
1. Using test script or multiple customer phones, generate 5 payments within 8 seconds:
   - Payment 1: ₹10
   - Payment 2: ₹20
   - Payment 3: ₹50
   - Payment 4: ₹100
   - Payment 5: ₹20
2. **Expected:**
   - First 2–3 payments announce individually or engine transitions into burst mode.
   - Subsequent rapid payments are grouped and summarized as:
     *"5 payments received, total ₹200."*
   - Prevents queue backlog and shopkeeper audio fatigue.

---

## 7. Diagnostics & Fixture Export

### Test 7.1: Redacted Log & Fixture Export
1. Receive a real payment from any app.
2. Navigate to **Diagnostics Screen**.
3. Tap **Export Redacted Fixture**.
4. Check output JSON:
   - Amounts and app package are intact.
   - Payer name is replaced or masked (e.g. `J**n D*e`).
   - Phone numbers, bank account numbers, and VPAs are completely masked (e.g. `98*****12@upi`).
5. Share or copy fixture to contribute to the open-source repository.

---

## 8. Security & Zero-Internet Verification

### Test 8.1: No Internet Permission
1. Inspect APK with `aapt2`:
   ```bash
   aapt2 dump permissions app-release.apk
   ```
2. Verify `android.permission.INTERNET` is **completely absent**.
3. Inspect network traffic using Wireshark / NetGuard:
   - Device produces 0 bytes of network egress from `com.upisoundbox.app` under any condition.

