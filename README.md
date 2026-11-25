# CinemaHub

CinemaHub is an experimental Android movie and TV show discovery app written in Kotlin with Jetpack Compose.

**Current status: v0.2 development preview (not build-verified).** The main tab loads trending movie and TV metadata from TMDB; the search tab searches TMDB; tapping an item displays its description. Demo data remains in the source code. Favorites, source adapters, and playback are not implemented.

## Getting started

1. Open the repository root in Android Studio.
2. Install Android SDK Platform 35 if prompted.
3. Set the Gradle JDK to a supported JDK (17 is recommended).
4. Sync Gradle.
5. Run the `app` configuration on an Android emulator or device (Android 8.0+).

The project does not yet include Gradle Wrapper files. Android Studio can synchronize using a local Gradle installation; adding and committing the complete wrapper is a priority before CI.

## TMDB setup

1. Obtain a **TMDB API Key (v3 auth)** from https://www.themoviedb.org/settings/api.
2. Run CinemaHub and enter the key on the **Каталог TMDB** screen. Do not paste it into GitHub.
3. Open **Поиск TMDB** to search titles, including TV series.

**Security note:** v0.2 stores the key in Android's app-private SharedPreferences, not in the GitHub repository. This is appropriate for an experimental local prototype but is **not encrypted storage**. The key is used in API query parameters, which may also be visible in network logs. For distribution, a backend proxy and secure credential architecture are recommended.

Data and image metadata are provided by TMDB. This product uses the TMDB API but is not endorsed or certified by TMDB.

## Roadmap

- Build verification for TMDB metadata, poster catalog and search; handle errors and caching
- Modular source interfaces for authorized providers and user-owned libraries
- Local favorites, history, and resume
- Android TV focus navigation
- Media3 playback for permitted streams
- CI Android APK build

## Security and licensing

Do not commit API keys, `local.properties`, local build artifacts, or signing keys.
Movie metadata is separate from access to video content. This project does not contain copyrighted video or tools to bypass access controls.

Project repository: https://github.com/Taktangel/CinemaHub
