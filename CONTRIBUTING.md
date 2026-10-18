# Contributing to Sentüp

Thanks for your interest in improving Sentüp! This is an open-source
(GPL-3.0) project and contributions are welcome.

## Getting set up

1. Install **JDK 17+** and the **Android SDK** (platform 30+).
2. Clone and build:
   ```bash
   git clone https://github.com/caca1403/sentup-android.git
   cd sentup-android
   ./gradlew assembleDebug
   ```
3. Run the checks before opening a pull request:
   ```bash
   ./gradlew :app:assembleDebug   # must build
   ./gradlew test                 # unit tests must pass
   ```

## Making a change

- Keep changes scoped and consistent with the surrounding code.
- Add or update tests for new pure logic (see `app/src/test`).
- Follow existing naming and Compose patterns.
- One logical change per pull request.

## Pull requests

- Describe *what* changed and *why*.
- Link any issue the PR addresses.
- Ensure CI passes.

## Reporting issues

Use the issue templates. Include your Android version, app version (Settings →
About) and steps to reproduce.

## License note

By contributing, you agree your contributions are licensed under the
**GNU GPL v3.0**, the same license as this project.
