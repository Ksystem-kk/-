# Copilot instructions

## Response language

- Respond to the user in Japanese by default, including explanations, summaries, and questions. Keep code, identifiers, commands, and quoted source text in their original language.

## Repository context

- This is a Kotlin Android app. `MainActivity` owns the CameraX preview and ML Kit QR scanning flow; `QrCodeValidator` checks the raw QR payload before the activity stores the first valid value and compares it with the next valid value.
- Accepted payloads start with `KKNI` and are 19 or 20 characters in total. Comparison is exact; do not normalize or persist scanned values.
- After the first accepted scan, require a frame with no detected QR before accepting the second, to prevent the same visible code from being compared with itself.

## Build, test, and lint

- Build: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Single test class: `./gradlew :app:testDebugUnitTest --tests 'com.example.qrcodematcher.QrCodeValidatorTest'`
- Lint: `./gradlew :app:lintDebug`
- JDK 17 and Android SDK platform 35 are required.
