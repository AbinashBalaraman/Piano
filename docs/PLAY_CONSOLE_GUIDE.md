# Google Play Console Publisher Runbook — Hamburg 274

This guide documents release signing, Android App Bundle (`.aab`) generation, Data Safety disclosures, and Google Play reviewer guidance for **Hamburg 274** (`com.hamburg274.piano`).

---

## 1. Release Signing & `.aab` Build Workflow

> **Security Rule:** Never hardcode keystore passwords in source files or commit production `.jks` / `.keystore` files to Git.

### Environment Variables for Production Release Signing
Before publishing to Google Play, configure your upload keystore via environment variables on your CI/local release workstation:

```bash
export RELEASE_STORE_FILE="/secure/path/hamburg274-upload.jks"
export RELEASE_STORE_PASSWORD="<your-keystore-password>"
export RELEASE_KEY_ALIAS="hamburg274-upload"
export RELEASE_KEY_PASSWORD="<your-key-password>"
```

### Build & Verify Commands
```bash
# 1. Run JVM unit tests (tuning, velocity, MIDI codec, 4D grader, update cooldown policy)
./gradlew :app:testDebugUnitTest

# 2. Build signed Release Android App Bundle (.aab) for Play Console upload
./gradlew :app:bundleRelease

# Output artifact:
# app/build/outputs/bundle/release/app-release.aab
```

### Certificate Fingerprints (Upload Key & Play App Signing)
1. Enable **Play App Signing** in Google Play Console → *Test and release → Setup → App signing*.
2. Record both SHA-1 and SHA-256 fingerprints if you later add optional cloud services (Firebase / Play Games Services):
   ```bash
   keytool -list -v -keystore "$RELEASE_STORE_FILE" -alias "$RELEASE_KEY_ALIAS"
   ```

---

## 2. Google Play Data Safety Form Answers

| Question | Exact Answer |
| :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **No** |
| **Is all of the user data collected by your app encrypted in transit?** | N/A (zero user data collected or transmitted) |
| **Do you provide a way for users to request that their data is deleted?** | N/A (all preferences & recordings stay strictly on-device; web deletion instructions provided for transparency) |
| **Does your app contain ads?** | **No** |
| **Does your app request dangerous Android permissions?** | **No** (zero runtime permissions declared) |

### Technical Verification
- `AndroidManifest.xml` declares **zero** `<uses-permission>` entries for microphone, camera, contacts, location, phone state, or broad media/storage access.
- Importing `.mid` / `.musicxml` scores and exporting `.mid` / 24-bit `.wav` recordings uses Android's zero-permission **Storage Access Framework** (`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`).
- Google Play Core `com.google.android.play:app-update:2.1.0` communicates solely with the Play Store app to check version availability.

---

## 3. App Access & Play Reviewer Notes

Paste the following into Play Console → *App content → App access*:

- **Selection:** *All functionality in my app is available without any access restrictions.*
- **Reviewer Notes:**
  > Hamburg 274 is a 100% offline classical concert grand piano synthesizer and Urtext practice app. No login, account, or network connection is required to use any feature.
  >
  > • Tap the full-screen landscape keybed (or slide the left CC64 column) to play immediately.
  > • Tap the **Practice** tab to audition curated public-domain classical pieces with the synchronized Urtext Grand Staff and Falling-Note Waterfall, or import a `.mid`/`.musicxml` file via Android's document picker.
  > • Tap **Studio** to audition the built-in demo take across any of the 5 voicings and export `.mid` or 24-bit `.wav` via Android SAF.
  > • Tap **MIDI** to view live audio telemetry, test 3-finger chord multi-touch, enter OLED Sound-Module mode, or manually check for updates. External USB OTG / Bluetooth LE MIDI controllers are optional.

---

## 4. Target Audience & Families Policy

- **Target Age Group:** `13–15`, `16–17`, `18 and over` (or `All Ages` — the app contains zero ads, zero social/chat features, zero third-party tracking, and collects zero personal information).
- **Appeals to Children:** If you opt into *Designed for Families*, note that Hamburg 274 already complies with COPPA, GDPR-K, and Google Play Families policies out of the box (zero data collection, zero ads, zero external links inside the playing stage).

---

## 5. Hosting Privacy Policy & Data Deletion Pages

Deploy `/public/privacy-policy.html` and `/public/delete-account.html` to any static HTTPS host (GitHub Pages, Cloudflare Pages, Firebase Hosting, or your domain) and enter those HTTPS URLs in Play Console → *App content → Privacy policy*.
