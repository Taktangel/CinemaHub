# CinemaHub

CinemaHub is an experimental Android movie and TV show discovery app written in Kotlin with Jetpack Compose.

**Current status: v0.3 development preview (not build-verified).** The main tab loads trending movie and TV metadata from TMDB; the search tab searches TMDB; tapping an item displays its description. Demo data remains in the source code. Favorites and provider-specific video integrations are not implemented. A small demo source registry and authorized stream player are present.

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

## v0.3 sources and player

- Open **Источники / Плеер** from the main app toolbar, or from a TMDB movie detail page.
- Switch the demo source or personal source on/off.
- Tap **Big Buck Bunny (MP4)** to test playback using the freely available Blender Foundation demo film.
- Optionally enter your own directly accessible **HTTPS** MP4, HLS (`.m3u8`) or DASH (`.mpd`) URL and tap Save.
- Tap a source to play. Position is saved locally on closing playback.
- Demo streams are **not** the video of the TMDB film you selected. TMDB only supplies metadata.
- Stream URLs must be from your own library or sources that permit you to play them. No DRM or paywall bypass functionality exists.
- API key and user-entered stream URL are stored in app-private SharedPreferences on device, **not encrypted**. Do not enter private credentials in stream URL query parameters.
- CI on this feature branch attempts a debug APK build via GitHub Actions. Treat the branch as experimental until the build passes.

### Adding a permitted source adapter

Create a Kotlin object implementing `StreamSource` in `SourcesScreen.kt`. Return `StreamItem` entries with an authorized HTTPS URL, then register it in `registeredSources`. The player automatically handles MP4, HLS and DASH. For streams requiring signed headers, OAuth, or DRM, use their documented supported integration rather than extracting URLs.

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
