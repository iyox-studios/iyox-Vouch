{
  description = "Fastlane and development environment for iyox-Vouch";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            android_sdk.accept_license = true;
            allowUnfree = true;
          };
        };

        androidComposition = pkgs.androidenv.composeAndroidPackages {
          platformVersions = [ "35" "36" ];
          buildToolsVersions = [ "35.0.0" "36.0.0" ];
          includeNDK = false;
        };

        androidSdk = androidComposition.androidsdk;
      in
      {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            fastlane
            sops
            ruby
            jdk21
            androidSdk
            chromium
	        gh
          ];

          env = {
            JAVA_HOME = "${pkgs.jdk21}";
            ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
            ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
            GRADLE_OPTS = "-Dorg.gradle.java.installations.auto-download=false -Dorg.gradle.java.installations.paths=${pkgs.jdk21}";
          };
        };
      }
    );
}
