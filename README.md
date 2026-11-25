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


## v0.4 TMDB networking and optional key

- When Android DNS resolves `api.themoviedb.org` or `image.tmdb.org` only to loopback, the app uses Quad9's `https://dns10.quad9.net/dns-query` via bootstrap addresses `9.9.9.10` and `149.112.112.10` for those hosts. HTTPS certificate validation is retained. Regular addresses use the Android DNS resolver. This cannot fix a blocked HTTPS connection or unavailable Quad9 service.
- TMDB v3 key entered in the app is retained across ordinary updates with the same application ID and signing certificate. Do **not** uninstall or clear app data if you want to retain it.
- To preconfigure a key in your *personal build*, set a repository **Actions secret** named `TMDB_API_KEY` at GitHub Settings → Secrets and variables → Actions. The workflow uses it for the build. Do not paste it into source code, commits, PRs or logs.
- **Important:** any key embedded in an APK can be extracted by recipients. This secret-based method keeps the key out of Git history but **does not make it secret inside the APK**. Avoid distributing personalized APKs; use a backend API for production. CI build artifacts may be accessible to others in a public repository.

## v0.5 external video player

In **Источники / Плеер**, each source now has **Внешний плеер** as the primary action. Android opens its standard app chooser (VLC or another installed app that accepts streaming links). The built-in Media3 player remains as a fallback. Using a different player does not fix a denied/slow HTTP source; try your own accessible stream if both demos fail.

### Stable APK signing for updates (configure once)

Android requires the same signing certificate for all updates. GitHub-hosted debug builds without a persistent keystore may be signed differently each time, which causes an "app not installed" or update conflict. **Never commit a keystore or passwords to your public repo.**

Generate a permanent keystore **locally on your Windows computer** using the JDK keytool (choose strong passwords and keep the JKS in a private backup outside the repo):

```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\\CinemaHub-release.jks" -alias cinemahub -keyalg RSA -keysize 3072 -validity 10000
```

Convert the file to Base64 text locally:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\\CinemaHub-release.jks")) | Set-Clipboard
```

At repository **Settings → Secrets and variables → Actions**, create these repository secrets:

- `CINEMAHUB_KEYSTORE_BASE64` = clipboard Base64 text from your private JKS.
- `CINEMAHUB_STORE_PASSWORD` = the keystore password.
- `CINEMAHUB_KEY_ALIAS` = `cinemahub`.
- `CINEMAHUB_KEY_PASSWORD` = key password (may equal store password, depending on keystore).

On subsequent builds, GitHub Actions decodes the JKS into runner temporary storage and uses it to sign the debug APK. Keep passwords and keystore backed up; loss of the signing key means no seamless updates. Do not share these secrets in issues or chats.

**Migration:** Existing 0.4.x builds used unrelated debug signing keys. You may need to uninstall once to migrate to the persistent key, which clears the current API key unless separately backed up. All updates signed with the new key can then install over the previous app, provided the application ID remains the same and `versionCode` increases.

**Do not distribute the persistent JKS.** For a public launch, consider production release signing, restricted CI environments, and protect the signing workflow from untrusted pull request code.


## v0.6 personal media library

- Tap **♡ В избранное** on a TMDB title details screen, then visit the heart tab to browse saved titles.
- Tap **Мои источники** from the TMDB details or favorites. Save an authorized HTTPS MP4/HLS/DASH link associated with *that* TMDB movie or TV title.
- Choose external or embedded player for the linked source. Opening the associated source records the title under recent history. Demo MP4 sources are separate and not associated with the title.
- Favorites, per-title links and recent selections are stored on the device. They survive ordinary signed updates but **not** uninstall, data-clear or moving to a new device.
- This does not discover or extract video URLs from third-party unlicensed streaming sites; TMDB is only a metadata source.
- v0.6 does not yet support TV-specific per-season/episode link mapping, cloud synchronization, or cross-device watch history.

## v0.7 JSON source adapter (prototype)

The **Настройки** (gear) screen now includes a configurable source adapter for a **user-controlled HTTPS JSON catalog**. The catalog is read at runtime, so adding or changing a catalog doesn't require rebuilding the app.

1. Publish a UTF-8 `catalog.json` file at a publicly accessible HTTPS address. You can use GitHub Pages, a server you administer, or another allowed HTTPS hosting service. Do not add usernames, passwords, or API tokens to the URL.
2. Use a document with the following structure:

```json
{
  "name": "Моя медиатека",
  "items": [
    {
      "tmdb_id": 550,
      "media_type": "movie",
      "title": "Демонстрационный ролик для проверки адаптера — не фильм",
      "url": "https://assets.testfiles.dev/video/sample-3s.mp4",
      "attribution": "Short, separately hosted test video; not the selected movie."
    }
  ]
}
```

3. Open the **Настройки** screen in CinemaHub 0.7, enable **JSON-адаптер**, enter the catalog URL, tap **Сохранить**, and then **Проверить каталог**.
4. Find the title with TMDB ID 550, open **Мои источники**. The example will be labeled as a test video, **not the actual film**. For other catalog entries use each work's correct TMDB ID, and only use streams you are authorized to provide or play.

The app supports **one configured JSON catalog** with up to 200 items per load and matches streams using `media_type` + `tmdb_id`. The JSON schema is a documented extension point: additional adapters may implement `CatalogAdapter`. No HTML scraping, login bypass, DRM circumvention or automatic extraction of third-party playback URLs is implemented.

**Limitations:** The app currently downloads the catalog each time the source screen is opened; it does not cache it offline. JSON errors are shown in the source screen or by testing from Settings. A public source URL must be treated as untrusted: configure only catalogs you control or trust. The project remains a personal prototype; it does not yet offer a sandbox for arbitrary executable third-party plugins.

**Signing:** v0.7 still depends on the persistent APK signing secrets described in v0.5. An unsigned or differently signed update will not install over an existing app.

## v0.8 website providers and AniLiberty search

The **Настройки → AnimeGO / AniLibria / HdRezkaApi / 4PDA** screen is a directory of external resources. Each entry can be shown/hidden and opened in the device browser.

- **AnimeGO**: opens `https://animego.me/`. No verified official API integration yet; no automated video extraction.
- **AniLiberty (formerly AniLibria)**: supports in-app title search via public `GET https://aniliberty.top/api/v1/app/search/releases?query=...` and opens matching releases on the provider's website. The old `anilibria.tv` domain now identifies itself as an archive. No in-app AniLiberty video stream integration yet.
- **HdRezkaApi**: links to `SuperZombi/HdRezkaApi` documentation. This project is a **Python** library and cannot be imported directly into Android/Kotlin. A separate backend or port would need verification, licensing and permission to access media.
- **4PDA topics 902011 and 1111927**: reference links only. Their content could not be inspected (HTTP 403). They are disabled by default.

Website navigation and an API title search are distinct from real video playback. The current playback screen still accepts explicitly authorized HTTPS video streams from your own library or JSON adapters. v0.8 does not bypass accounts, paywalls, DRM or regional/network restrictions.

The public AniLiberty API search implementation is preliminary and depends on the provider's current response schema and availability. If it returns a networking error, the app will display the error rather than silently substituting a scraped source.

### Version sequencing

Version is `0.8.0` with `versionCode=9`. This change is stacked on the 0.7 feature branch. Merge stacked PRs in sequence if you want the new files in `main`. The persistent signing secret setup is still required for seamless APK updates.

### AniLibria documentation version notice

The user's reference repository [anilibria/docs](https://github.com/anilibria/docs) explicitly marks its historical `api.md`, `api_v2.md` and `api_v3.md` APIs as **DEPRECATED / retired**. Its README redirects developers to the current [AniLiberty API v1 documentation](https://anilibria.top/api/docs/v1). We intentionally use the modern public `GET /api/v1/app/search/releases?query=...` endpoint (AniLiberty host) for title search. The v1 API returns a JSON array of release objects. This implementation currently displays results and opens the provider site; it does not retrieve or play episodes. Verify public endpoint access and provider terms before expanding playback functionality.

## v0.9 — AniLiberty episodes + preferred video player

- From **Настройки → Каталог сайтов и API → AniLiberty → Поиск**, enter an anime title, choose a release, choose stream quality **Авто / 1080p / 720p / 480p**, and tap **▶ Смотреть серию**.
- Uses the public API v1 endpoints `GET /app/search/releases?query=...`, `GET /anime/releases/{idOrAlias}` and `GET /anime/releases/episodes/{releaseEpisodeId}` under `https://aniliberty.top/api/v1`. The final episode request supplies up-to-date HLS URLs; not every quality exists for every episode.
- **Настройки → Плеер по умолчанию**: select an installed VLC, Just Player, MX Player (regular/Pro), mpv-android, built-in player or **Спрашивать каждый раз**. Installed apps are detected by narrow Android `<queries>` package declarations. The preference persists in app-private data.
- All streams on the **Источники** screen also use the chosen default player via **▶ Смотреть**; the direct built-in playback button remains available as a fallback.
- If the selected external player was removed or can't handle the URL, CinemaHub shows Android's player chooser instead of silently failing.
- AniLiberty is an external service. Playback depends on its server, the user's region/network and availability of CDN HLS streams. The app does not decrypt DRM, obtain private credentials, or bypass access control. Playback availability has not been confirmed on a physical phone.
- Stable APK signing still requires an owner-controlled keystore in GitHub Actions secrets; new debug builds without one may require a one-time uninstall and will clear local preferences.
