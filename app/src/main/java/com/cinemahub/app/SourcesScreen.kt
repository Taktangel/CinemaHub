package com.cinemahub.app

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A stream must be publicly playable or owned/authorized by the user. */
data class StreamItem(
    val id: String,
    val title: String,
    val url: String,
    val source: String,
    val attribution: String = ""
)

/** New providers implement this interface without modifying the player. */
interface StreamSource {
    val id: String
    val label: String
    fun streams(context: Context): List<StreamItem>
}

private object DemoSource : StreamSource {
    override val id = "demo"
    override val label = "Тестовые MP4-видео"
    override fun streams(context: Context) = listOf(
        StreamItem(
            id = "samplelib-5s",
            title = "SampleLib — 5 секунд (MP4)",
            url = "https://samplelib.com/lib/preview/mp4/sample-5s.mp4",
            source = label,
            attribution = "Тестовый ролик SampleLib, не связан с каталогом TMDB"
        ),
        StreamItem(
            id = "testfiles-3s",
            title = "TestFiles.dev — 3 секунды (MP4)",
            url = "https://assets.testfiles.dev/video/sample-3s.mp4",
            source = label,
            attribution = "Альтернативный тестовый MP4, не связан с TMDB"
        )
    )
}

private object UserSource : StreamSource {
    override val id = "user"
    override val label = "Моя ссылка"
    override fun streams(context: Context): List<StreamItem> {
        val prefs = context.getSharedPreferences("cinema_sources", Context.MODE_PRIVATE)
        val url = prefs.getString("custom_url", "").orEmpty().trim()
        if (!isSupportedStreamUrl(url)) return emptyList()
        return listOf(StreamItem("custom", "Мой видеопоток", url, label))
    }
}

private val registeredSources: List<StreamSource> = listOf(DemoSource, UserSource)

/** Only explicit HTTPS urls; no scraping, DRM bypass or extracted media links. */
fun isSupportedStreamUrl(input: String): Boolean {
    if (input.isBlank()) return false
    val uri = Uri.parse(input)
    return uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
}

@Composable
fun SourcesScreen(onPlay: (StreamItem) -> Unit, onBack: () -> Unit, movie: LibraryMovie? = null, onPlayed: (LibraryMovie) -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("cinema_sources", Context.MODE_PRIVATE) }
    var demoEnabled by remember { mutableStateOf(prefs.getBoolean("demo_enabled", true)) }
    var userEnabled by remember { mutableStateOf(prefs.getBoolean("user_enabled", true)) }
    var customUrl by remember { mutableStateOf(prefs.getString("custom_url", "").orEmpty()) }
    var savedUrl by remember { mutableStateOf(customUrl) }
    var playerMessage by remember { mutableStateOf<String?>(null) }
    var movieUrl by remember(movie?.mediaKey) { mutableStateOf(movie?.let { MediaLibrary.videoUrl(context, it.mediaKey) }.orEmpty()) }
    var savedMovieUrl by remember(movie?.mediaKey) { mutableStateOf(movieUrl) }
    val ownFilmStream = if (movie != null && isSupportedStreamUrl(savedMovieUrl)) {
        listOf(StreamItem("linked-${movie.mediaKey}", movie.title, savedMovieUrl, "Мой источник для фильма"))
    } else emptyList()
    val playable = ownFilmStream + registeredSources
        .filter { source -> when (source.id) {
            "demo" -> demoEnabled
            "user" -> userEnabled
            else -> false
        } }
        .flatMap { it.streams(context) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Назад") }
        Text(movie?.let { "Источники: ${it.title}" } ?: "Источники и плеер", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Это тестовые видеопотоки, не привязанные к фильмам TMDB. " +
                "TMDB предоставляет метаданные, а не видео.",
            color = Color.LightGray
        )
        Spacer(Modifier.height(12.dp))
        if (movie != null) {
            OutlinedTextField(
                value = movieUrl,
                onValueChange = { movieUrl = it },
                label = { Text("Моя HTTPS-ссылка для фильма") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = movieUrl.isBlank() || isSupportedStreamUrl(movieUrl.trim()),
                onClick = {
                    val url = movieUrl.trim()
                    MediaLibrary.setVideoUrl(context, movie.mediaKey, url)
                    savedMovieUrl = url
                }
            ) { Text("Сохранить для этого фильма") }
            Text("Видеоссылка хранится локально и не извлекается из TMDB.", color = Color.LightGray)
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Демоисточник", color = Color.White)
            Switch(checked = demoEnabled, onCheckedChange = {
                demoEnabled = it
                prefs.edit().putBoolean("demo_enabled", it).apply()
            })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Собственный поток", color = Color.White)
            Switch(checked = userEnabled, onCheckedChange = {
                userEnabled = it
                prefs.edit().putBoolean("user_enabled", it).apply()
            })
        }
        OutlinedTextField(
            value = customUrl,
            onValueChange = { customUrl = it },
            label = { Text("HTTPS MP4 / HLS .m3u8 / DASH .mpd") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(6.dp))
        Button(
            enabled = customUrl.isBlank() || isSupportedStreamUrl(customUrl.trim()),
            onClick = {
                val url = customUrl.trim()
                prefs.edit().putString("custom_url", url).apply()
                savedUrl = url
            }
        ) { Text("Сохранить ссылку") }
        if (customUrl.isNotBlank() && !isSupportedStreamUrl(customUrl.trim())) {
            Text("Нужна корректная HTTPS-ссылка без логина и пароля в адресе.", color = MaterialTheme.colorScheme.error)
        }
        if (savedUrl.isNotBlank() && !playable.any { it.id == "custom" } && userEnabled) {
            Text("Сохрани поддерживаемую ссылку, чтобы она появилась в списке.", color = Color.LightGray)
        }
        Spacer(Modifier.height(12.dp))
        Text("Доступные источники", color = Color.White, style = MaterialTheme.typography.titleMedium)
        playerMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (playable.isEmpty()) Text("Источников пока нет", color = Color.LightGray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(playable, key = { it.id }) { stream ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(stream.title, style = MaterialTheme.typography.titleMedium)
                        Text(stream.source, style = MaterialTheme.typography.bodySmall)
                        if (stream.attribution.isNotBlank()) Text(stream.attribution, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            playerMessage = try {
                                openInExternalPlayer(context, stream.url)
                                if (movie != null && stream.id.startsWith("linked-")) onPlayed(movie)
                                null
                            } catch (_: ActivityNotFoundException) {
                                "На устройстве не найден совместимый видеоплеер. Установи VLC или другой плеер."
                            } catch (e: Exception) {
                                "Не удалось открыть внешнее приложение: ${e.message}"
                            }
                        }) { Text("▶ Внешний плеер") }
                        TextButton(onClick = {
                            if (movie != null && stream.id.startsWith("linked-")) onPlayed(movie)
                            onPlay(stream)
                        }) {
                            Text("Встроенный плеер (резервный)")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Send the video URL to an installed Android media application.
 * Nothing is downloaded or extracted. Players are responsible for streaming.
 */
private fun openInExternalPlayer(context: Context, url: String) {
    val path = Uri.parse(url).path.orEmpty().lowercase()
    val type = when {
        path.endsWith(".m3u8") -> "application/vnd.apple.mpegurl"
        path.endsWith(".mpd") -> "application/dash+xml"
        else -> "video/*"
    }
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(url), type)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(view, "Выбери видеоплеер").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
