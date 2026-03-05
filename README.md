# Cryptora - Offline Password Vault for Android

## 1) What is Cryptora?

Cryptora is a private password manager that works fully offline on your Android phone.

You can store login details (like app/site name, username, password, notes, and links) in one secure place and unlock everything with a **master password** that only you know.

Because it works offline, your data is not uploaded to cloud servers by this app.

---

## 2) Features

### Core vault experience
- Store and manage passwords, usernames, URLs, notes, and categories.
- Fast search to find saved entries quickly.
- Clean add/edit/view flow for credentials.

### Master password protection
- On first launch, you create a **master password**.
- This master password is required to unlock the vault.
- Your vault data is saved in encrypted form on device storage.

### Backup and restore (with encrypted file)
- You can back up your vault to a file.
- Backup file is encrypted before it is saved.
- During restore, the app decrypts that file using your master password.
- If the password is wrong (or file is damaged), restore will fail safely.

### Extra protection
- Optional PIN support.
- Optional biometric unlock (device support required).
- Auto-lock after inactivity/background.
- Copy-to-clipboard auto-clear.
- Screenshot blocking on sensitive screens.

### Privacy-first
- Designed for offline use.
- No internet permission in normal use.

---

## 3) Setup Guide (ZIP -> APK -> Install)

Follow these steps if you downloaded the project as a ZIP and want to install the app APK.

### Step A: Download and extract
1. Download the project ZIP.
2. Extract it (unzip) to a folder on your computer.
3. Open terminal/command prompt in that extracted folder.

### Step B: Build APK
> Requirement: Android SDK + JDK 17 installed.

Run:

```bash
./gradlew :app:packageCryptoraApk
```

After build completes, APK will be available at:

```text
app/build/outputs/apk/cryptora/release/Cryptora.apk
```

### Step C: Move APK to your phone
Use any one method:
- USB file transfer
- Nearby Share
- Cloud drive (upload/download manually)

### Step D: Install on Android
1. On your phone, open the APK file.
2. If prompted, allow installation from this source.
3. Continue and install.

If install is blocked:
- Remove older Cryptora app first (if signed differently).
- Make sure file name is `Cryptora.apk` from the latest build.
- Rebuild using the command above and copy again.

---

## First-time use
1. Open Cryptora.
2. Set your master password.
3. (Optional) Enable PIN/biometric.
4. Start adding your credentials.
5. Create a backup file and keep it safe.

---

## Important notes
- If you forget your master password, you cannot unlock encrypted vault data.
- Keep your backup file and master password safe.
- For regular users, installing APK from project releases/artifacts is easier than local building.
