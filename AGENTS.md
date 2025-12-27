# Repository Guidelines

## Project Structure & Module Organization
- `app/` is the single Android application module; Gradle config lives in `app/build.gradle.kts` with shared versions in `gradle/libs.versions.toml`.
- Production source is under `app/src/main`: Kotlin in `app/src/main/java/com/porarrirr/sumahohikakuku`, resources in `app/src/main/res/`.
- Unit tests are in `app/src/test`, instrumentation tests in `app/src/androidTest` (mirror the main package tree).
- Keep root Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) committed for reproducible builds.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` — builds a debug APK to catch compile errors early.
- `./gradlew test` — runs local JVM unit tests in `app/src/test`.
- `./gradlew connectedAndroidTest` — runs instrumentation tests on a device/emulator.
- `./gradlew lint` — runs Android Lint; fix or document warnings before PRs.

## Coding Style & Naming Conventions
- Kotlin preferred for new code; 4-space indentation; lines ≤ 120 chars.
- Naming: `camelCase` for variables/functions, `PascalCase` for classes.
- Add concise KDoc for non-trivial logic.
- Resources use `snake_case` (e.g., `activity_main.xml`, `ic_sensor_light.xml`); string IDs should describe intent (`label_device_model`).
- XML attributes follow Android Studio’s default ordering; reformat with **Code > Reformat File**.

## Testing Guidelines
- Use JUnit 4 for unit tests; file names like `<Feature>Test.kt`.
- Test methods use `fun <methodUnderTest>_<expected>()` (e.g., `fun parseInput_returnsNull()`).
- Instrumentation tests validate UI/Android APIs; suffix with `InstrumentedTest`.
- Aim for ≥80% coverage for new core utilities; note exceptions in the PR.
- Use `@Before`/`@After` to isolate state and clean up resources.

## Commit & Pull Request Guidelines
- Use Conventional Commits (e.g., `feat:`, `fix:`, `docs:`); one feature/fix per commit.
- PRs include: clear summary, testing notes (e.g., `test: ./gradlew test`), and issue references.
- Attach screenshots or recordings for UI changes; mention impacted SDK levels/devices.

## Agent-Specific Notes
- Keep changes scoped; do not remove existing files unless requested.
- If you spot unexpected modifications you didn’t make, stop and ask how to proceed.
