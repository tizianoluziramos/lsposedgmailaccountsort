# Gmail Account Sort

An LSPosed module that sorts Gmail accounts alphabetically (A–Z) by email address in the account switcher. Sorting is case-insensitive and preserves the active account, avatars, and account selection functionality.

## Installation

1. Install `GmailAccountSort.apk`.
2. Enable **Gmail Account Sort** in LSPosed Manager.
3. Select **Gmail** as the only target application.
4. Force stop Gmail and reopen it.

## Compatibility

Supported Gmail versions:

- `2026.11.02.1015914183.Release`
- `2026.11.09.1019722178.Release` and above

Supports both classic and Bento account switcher layouts.

## Build

Requires Android SDK 35, JDK 17, and Gradle 8.9.

**Windows:**
```powershell
.\gradlew.bat :app:assembleRelease
```

**Linux / macOS:**
```bash
./gradlew :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

## License

No license specified.