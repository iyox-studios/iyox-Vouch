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

### android publish_store_listing

```sh
[bundle exec] fastlane android publish_store_listing
```

Upload all store listing assets (metadata, icon, feature graphic, screenshots) from repo to Google Play without a new release

### android publish_screenshots

```sh
[bundle exec] fastlane android publish_screenshots
```

Upload screenshots only to Google Play without a new release

### android pull_metadata

```sh
[bundle exec] fastlane android pull_metadata
```

Pull current store listing metadata and graphics from Google Play Store into repo

### android screenshots

```sh
[bundle exec] fastlane android screenshots
```

Generate localized screenshots for all configured locales via screengrab

### android frame_screenshots

```sh
[bundle exec] fastlane android frame_screenshots
```

Frame raw screenshots with Material 3 Expressive containers and titles for Google Play

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
