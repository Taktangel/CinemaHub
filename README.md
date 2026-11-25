# CinemaHub

CinemaHub is an experimental Android movie and TV show discovery app written in Kotlin with Jetpack Compose.

**Current status: v0.1.0 prototype.** The app displays six built-in demo entries and can search them locally. Favorites, source adapters, video playback, and a TMDB integration are not implemented yet.

## Getting started

1. Open the repository root in Android Studio.
2. Install Android SDK Platform 35 if prompted.
3. Set the Gradle JDK to a supported JDK (17 is recommended).
4. Sync Gradle.
5. Run the `app` configuration on an Android emulator or device (Android 8.0+).

The project does not yet include Gradle Wrapper files. Android Studio can synchronize using a local Gradle installation; adding and committing the complete wrapper is a priority before CI.

## Roadmap

- TMDB metadata and poster catalog (respecting the TMDB API terms and attribution requirements)
- Modular source interfaces for authorized providers and user-owned libraries
- Local favorites, history, and resume
- Android TV focus navigation
- Media3 playback for permitted streams
- CI Android APK build

## Security and licensing

Do not commit API keys, `local.properties`, local build artifacts, or signing keys.
Movie metadata is separate from access to video content. This project does not contain copyrighted video or tools to bypass access controls.

Project repository: https://github.com/Taktangel/CinemaHub
