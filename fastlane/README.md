fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android build_release

```sh
[bundle exec] fastlane android build_release
```

Build the signed Android App Bundle (.aab)

### android release

```sh
[bundle exec] fastlane android release
```

Build and upload release AAB to Google Play Store (default track: internal)

### android screenshots

```sh
[bundle exec] fastlane android screenshots
```

Generate localized screenshots for all configured locales via screengrab

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
