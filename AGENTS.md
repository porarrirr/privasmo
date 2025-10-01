# Repository Guidelines

## Project Structure & Module Organization
- `app/` holds the single Android application module; Gradle config lives in `app/build.gradle.kts` with shared versions in `gradle/libs.versions.toml`.
- Source is under `app/src/main`; Kotlin code should live in `java/com/porarrirr/sumahohikakuku`, resources in `res/` (values, drawables, XML configs).
- Unit tests reside in `app/src/test`, instrumentation tests in `app/src/androidTest`; mirror the main package tree for easy navigation.
- Root-level Gradle wrapper files (`gradlew*`) must remain committed so contributors can build without a global Gradle install.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds a debug APK; run it before submitting changes to catch compile errors.
- `./gradlew test` executes local JVM unit tests in `app/src/test`.
- `./gradlew connectedAndroidTest` runs device/emulator instrumentation tests from `app/src/androidTest`.
- `./gradlew lint` runs Android Lint; fix or justify warnings before opening a PR.

## Coding Style & Naming Conventions
- Kotlin files use 4-space indentation, `camelCase` for functions/variables, and `PascalCase` for classes; keep lines ≤ 120 chars.
- Prefer Kotlin over Java for new code; add concise KDoc when logic is non-trivial.
- Resource naming: `snake_case` (e.g., `activity_main.xml`, `ic_sensor_light.xml`); string IDs should describe intent (`label_device_model`).
- XML attributes are ordered per Android Studio defaults; reformat with `Code > Reformat File` before committing.

## Testing Guidelines
- Add unit tests for pure logic using JUnit 4; name files `<Feature>Test.kt` and methods `fun <methodUnderTest>_<expected>()`.
- Instrumentation tests should validate UI or Android-specific APIs; suffix classes with `InstrumentedTest`.
- Maintain ≥80% coverage for new core utilities; justify lower coverage in the PR description when unavoidable.
- Use `@Before`/`@After` hooks to isolate state and clean up resources.

## Commit & Pull Request Guidelines
- Git history is currently empty; adopt Conventional Commits (`feat:`, `fix:`, `docs:`) to seed a consistent log.
- Commits should be scoped: one feature or fix per commit, including related tests/resources.
- PRs must include a clear summary, testing notes (`test: ./gradlew test`), and references to issues or feature requests.
- Attach screenshots or screen recordings for UI changes; mention impacted SDK levels or devices when relevant.
