# Contributing to iyox Vouch

Thanks for taking the time to contribute to iyox Vouch. Here are a few guidelines to help you get
started with development, code style, and submitting pull requests.

## Development Setup

Requirements:

- Android Studio
- JDK 17 or 21
- Android SDK 35

To build the project locally:

```bash
./gradlew assembleDebug
```

## Running Tests

This project has both local unit tests and instrumented/UI tests:

- **Local unit tests** (run on JVM, no device needed):
  ```bash
  ./gradlew test
  ```
- **Instrumented and UI tests** (Room DAO tests, Compose UI tests, and integration tests in
  `androidTest`):
  These require a running Android emulator or connected device:
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```

Make sure your changes don't break existing tests before submitting a pull request.

## Commit Messages

It's good practice to follow the [Conventional Commits](https://www.conventionalcommits.org/)
guidelines to keep the git history clean.
If you do, please use the imperative mood (e.g. "add", not "added" or "adds").

Format: `type(scope): description`

Examples:

- `feat(vouch): add quick currency picker to group vouchers`
- `fix(navigation): prevent tab flicker on startup`

## Code Style

- **Kotlin**: Follow the official Android Kotlin Style Guide. Use Android Studio's default code
  formatter (`Ctrl + Alt + L` / `Cmd + Option + L`).
- **Compose**:
    - UI composable functions should be named in `PascalCase` and accept
      `modifier: Modifier = Modifier` as the first optional parameter.
    - Hoist state where practical; composables should ideally be stateless and receive events via
      callbacks.
- **Architecture**:
    - Unidirectional Data Flow (UDF): ViewModels expose a single `uiState: StateFlow` to the screen.
    - Room is the single source of truth for persistent data.
- **Strings**:
    - Do not hardcode user-facing strings in UI code. Use `stringResource(R.string...)`.
    - Add new strings to `app/src/main/res/values/strings.xml`.

## Translating

Adding or updating translations requires no coding, just some git knowledge:

1. **Locate the base strings**: The default (English) strings are located at
   `app/src/main/res/values/strings.xml`.
2. **Add or update your language**:
    - To update an existing translation (e.g. German), edit
      `app/src/main/res/values-de/strings.xml`.
    - To add a new language, create a new folder `app/src/main/res/values-<language_code>/` (for
      example, `values-es` for Spanish or `values-fr` for French) and copy `strings.xml` into it.
3. **Translate the strings**: Translate the text content inside `<string name="...">...</string>`
   tags while keeping placeholders (like `%s`, `%d`) intact.
4. **Submit a PR**: Open a Pull Request on GitHub with your new or updated `strings.xml` file.

## Pull Request Process

1. Create a branch from `main` (e.g. `feature/my-feature` or `fix/issue-description`).
2. Make your changes and verify that both builds and relevant tests pass.
3. Open a pull request against `main` with a short summary of what you changed and why.

## License

By contributing, you agree that your contributions will be licensed under the project's GPL v3.0
license.
