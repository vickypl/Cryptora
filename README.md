# Cryptora - Offline Android Password Vault

Cryptora is a fully offline password vault built with Kotlin, Jetpack Compose, MVVM, Room, SQLCipher, and Android Keystore.

## Features

- 100% offline operation with **no internet permissions**.
- Master password setup with PBKDF2WithHmacSHA256 (210,000 iterations).
- Encrypted SQLCipher database (`vault_encrypted.db`).
- Keystore-based wrapping of database key.
- Optional PIN fallback and lockout after 5 failed attempts (30 seconds).
- Biometric permission support (`USE_BIOMETRIC`) and settings toggle.
- Clipboard auto-clear after 15 seconds for copied passwords.
- Auto-lock on background and inactivity.
- Root detection warning.
- Screenshot hardening via `FLAG_SECURE`.
- Min SDK 26, target SDK 35.

## Architecture

- **MVVM**:
  - `SessionViewModel` controls lock/unlock session state.
  - `VaultViewModel` handles credential CRUD and filtering.
- **Data layer**:
  - Room DAO/entity in `data/local`.
  - Repository abstraction in `data/repository`.
- **Security layer**:
  - PBKDF2 derivation, encrypted shared prefs, keystore wrapping, root checks.
- **UI**:
  - Compose screens: setup, unlock, vault list, add credential, detail dialog.

## Security Model

1. User sets master password on first launch.
2. App generates 32-byte salt and derives hash via PBKDF2-HMAC-SHA256.
3. App generates random 32-byte SQLCipher key.
4. SQLCipher key is wrapped with Android Keystore AES key.
5. Only salt, master hash, wrapped key, and IV are persisted in encrypted preferences.
6. Master password is never stored plaintext.

## Setup & Build

### Requirements

- Android Studio Koala or newer
- JDK 17
- Android SDK 35

### Build

```bash
./gradlew assembleRelease
```

### Run tests

```bash
./gradlew test
```

## Usage Guide

1. Launch app and create a master password (PIN optional).
2. Unlock with master password/PIN.
3. Add credentials using the FAB.
4. Use search to filter entries.
5. Open detail and copy password to clipboard (auto-clears in 15s).
6. Toggle biometric option in vault home settings row.

## Permissions

Allowed:
- `android.permission.USE_BIOMETRIC`

Not present:
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`

## Notes

- Backup/import/export screen hooks are planned and should use encrypted `.enc` files in app-private storage only.
- Release builds enable R8/ProGuard.
