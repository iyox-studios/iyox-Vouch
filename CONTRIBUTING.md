# Contributing to iyox Vouch

Thanks for taking the time to contribute to iyox Vouch. Here are a few guidelines to help you get started with development, code style, and submitting pull requests.

## Development Setup

Requirements:
- Android Studio
- JDK 17 or 21
- Android SDK 36

To build the project locally:
```bash
./gradlew assembleDebug
```

## Running Tests

This project contains both local JVM tests and on-device instrumented tests:

- **Local unit tests** (runs on JVM without an emulator):
  ```bash
  ./gradlew test
  ```
  *(or `./gradlew testDebugUnitTest`)*

- **Instrumented and UI tests** (Room DAO tests, Compose UI tests, and integration tests in `androidTest`):
  Requires a running Android emulator or connected device:
  ```bash
  ./gradlew connectedCheck
  ```
  *(or `./gradlew connectedDebugAndroidTest`, or right-click `androidTest` in Android Studio and select "Run 'All Tests'")*

Make sure your changes don't break existing tests before submitting a pull request.

## Static Analysis (Qodana)

We use JetBrains Qodana to maintain code quality. Before submitting a pull request, run:

```bash
./gradlew qodanaScan
```

The scan should pass and produce 0 new findings.

## Commit Messages

It's good practice to follow the [Conventional Commits](https://www.conventionalcommits.org/) guidelines to keep the git history clean. If you do, please use the imperative mood (e.g. "add", not "added" or "adds").

Format: `type(scope): description`

Examples:
- `feat(vouch): add quick currency picker to group vouchers`
- `fix(navigation): prevent tab flicker on startup`

## Code Style

- **Kotlin**: Follow the official Android Kotlin Style Guide. Use Android Studio's default code formatter.
- **Compose**:
  - UI composable functions should be named in `PascalCase` and accept `modifier: Modifier = Modifier` as the first optional parameter.
  - Hoist state where practical; composables should ideally be stateless and receive events via callbacks.
- **Architecture**:
  - Unidirectional Data Flow (UDF): ViewModels expose a single `uiState: StateFlow` to the screen.
  - Room is the single source of truth for persistent data.
- **Strings**:
  - Do not hardcode user-facing strings in UI code. Use `stringResource(R.string...)`.
  - Add new strings to `app/src/main/res/values/strings.xml`.

## Icons

The Material Symbols assets in `app/src/main/res/drawable/ic_*.xml` come from
Google's [material-design-icons](https://github.com/google/material-design-icons)
repository, revision `10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7`. They use Apache-2.0;
the attribution and full license are packaged in
[material_symbols_license.txt](app/src/main/res/raw/material_symbols_license.txt).
Each icon XML records its source.

Use Material Symbols Outlined, weight 400, optical size 24dp. Navigation selected
states use FILL=1; other icons use FILL=0. Compose controls tint, so remove theme
tint from imported assets. Preserve `android:autoMirrored="true"` for mirrored
icons and enable it where appropriate for directional navigation.

To add an icon, download Android XML from [Google Fonts Icons](https://fonts.google.com/icons)
and place it in `app/src/main/res/drawable/ic_name.xml`. Use it in Compose with
`Icon(painter = painterResource(R.drawable.ic_name), contentDescription = ...)`.
Use the same style, weight, and size as the existing icons. SVG downloads must
first be imported as vector drawables using Android Studio's **New > Vector Asset > Local file**.

## Translating

Adding or updating translations requires no coding, just some git knowledge:

1. **Locate the base strings**: The default (English) strings are located at `app/src/main/res/values/strings.xml`.
2. **Add or update your language**:
   - To update an existing translation (e.g. German), edit `app/src/main/res/values-de/strings.xml`.
   - To add a new language, create a new folder `app/src/main/res/values-<language_code>/` (for example, `values-es` for Spanish or `values-fr` for French) and copy `strings.xml` into it.
3. **Translate the strings**: Translate the text content inside `<string name="...">...</string>` tags while keeping placeholders (like `%s`, `%d`) intact.
4. **Register the locale**: When adding a new language, add your locale code (e.g. `"es-rES"`) to `localeFilters` in `app/build.gradle.kts` so the translation is packaged into the build.
5. **Submit a PR**: Open a Pull Request on GitHub with your changes.

## Pull Request Process

1. Create a branch from `main` (e.g. `feature/my-feature` or `fix/issue-description`).
2. Make your changes and verify that builds, relevant tests, and `./gradlew qodanaScan` pass without new findings.
3. Open a pull request against `main` with a short summary of what you changed and why.

## AI-Assisted Contributions

AI tools are welcome to assist development, but you are fully responsible for your submitted changes. Please ensure all code is reviewed, understood, and thoroughly tested before opening a pull request.

## License

By contributing, you agree that your contributions will be licensed under the project's **[GNU General Public License v3.0](LICENSE)**
