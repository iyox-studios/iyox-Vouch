<div align="center">

<div align="center">
    <img width="200" height="200" style="display: block; border-radius: 9999px;" src="assets/icon/icon.svg" alt="app icon">
</div>

---
# iyox Vouch

iyox Vouch is a personal debt and IOU tracking application presented
in [Material You Expressive](https://m3.material.io/) style.

</div>

## Screenshots

[<img width=200 alt="Vouches Overview" src="assets/screenshots/Vouches%20Overview.png">]()
[<img width=200 alt="Add Vouch" src="assets/screenshots/Add%20Vouch.png">]()
[<img width=200 alt="Persons Overview" src="assets/screenshots/Persons%20Overview.png">]()
[<img width=200 alt="Person Overview" src="assets/screenshots/Person%20Overview.png">]()

## Features

- **Debt & IOU Tracking**: Easily track money borrowed or lent with amounts, dates, and
  descriptions.
- **Group Vouches**: Split expenses and bills across multiple people effortlessly.
- **People Overview**: View individual balances, net debts/credits, and settle balances with one
  tap.
- **Material You Expressive**: Clean, modern interface with dynamic colors and dark theme support.
- **Multi-Currency & Multilingual**: Customizable currency selection and multilingual support (
  English, German).
- **Data Backup & Restore**: Export and import your data in JSON format for safe keeping and easy
  migration.
- **Biometric Security**: Protect your private financial records with biometric authentication.

## Used Libraries

### Android & Jetpack Compose

* [Jetpack Compose](https://developer.android.com/jetpack/compose)(Apache-2.0) - Modern toolkit for
  building native Android UI
* [Material 3](https://developer.android.com/jetpack/androidx/releases/compose-material3)(
  Apache-2.0) - Material You UI components for Compose
* [Room](https://developer.android.com/jetpack/androidx/releases/room)(Apache-2.0) - SQLite object
  mapping and persistence library
* [Navigation Compose](https://developer.android.com/jetpack/androidx/releases/navigation)(
  Apache-2.0) - Navigation component for Jetpack Compose
* [Lifecycle & ViewModel](https://developer.android.com/jetpack/androidx/releases/lifecycle)(
  Apache-2.0) - Lifecycle management and state handling
* [Biometric](https://developer.android.com/jetpack/androidx/releases/biometric)(Apache-2.0) -
  Biometric authentication prompt
* [Gson](https://github.com/google/gson)(Apache-2.0) - JSON serialization and deserialization
  library
* [Core SplashScreen](https://developer.android.com/jetpack/androidx/releases/core)(Apache-2.0) -
  Splash screen compatibility library

## Contributing

Contributions are always very welcome.

You can contribute by:
- **Reporting Issues**: If you find a bug or have an idea: Open an issue on GitHub.
- **Submitting Pull Requests**: Just open a PR on GitHub with you changes.
- **Translating**: Help us reach everyone! See the [Translating](#translating) section below for instructions.

## Translating

Adding or updating translations is relatively straightforward and requires no coding, just some git knowledge:

1. **Locate the base strings**:
   The default (English) strings are located at:
   `app/src/main/res/values/strings.xml`
2. **Add or update your language**:
   - To update an existing translation (e.g. German), edit `app/src/main/res/values-de/strings.xml`.
   - To add a new language, create a new folder under `app/src/main/res/values-<language_code>/` (for example, `values-es` for Spanish or `values-fr` for French) and copy `strings.xml` into it.
3. **Translate the strings**:
   Translate the text content inside `<string name="...">...</string>` tags while keeping placeholders (like `%s`, `%d`) intact.
4. **Submit a PR**:
   Open a Pull Request on GitHub with your new or updated `strings.xml` file.

## License

iyox Vouch is licensed under the **[GPL v3.0 only](https://spdx.org/licenses/GPL-3.0-only.html)**.
