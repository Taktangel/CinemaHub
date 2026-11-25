package com.cinemahub.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class AniLibertyRelease(val id: Int, val title: String, val alias: String?, val poster: String? = null)

internal object AniLibertyApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun latest(): List<AniLibertyRelease> = withContext(Dispatchers.IO) {
        val url = "https://aniliberty.top/api/v1/anime/releases/latest?limit=15"
        val request = Request.Builder().url(url).header("Cache-Control", "no-cache").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("AniLiberty: HTTP ${response.code}")
            val json = response.body?.string() ?: throw IOException("Пустой ответ")
            val array = JSONArray(json)
            buildList {
                for (i in 0 until minOf(15, array.length())) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optInt("id")
                    if (id < 1) continue
                    val title = obj.optJSONObject("name")?.optString("main").orEmpty()
                        .ifBlank { "Аниме #$id" }
                    val alias = obj.optString("alias").takeIf { it.matches(Regex("[a-zA-Z0-9_-]+")) }
                    val preview = obj.optJSONObject("poster")?.optString("preview").orEmpty()
                    val poster = when {
                        preview.startsWith("https://") -> preview
                        preview.startsWith("/storage/") -> "https://aniliberty.top" + preview
                        else -> null
                    }
                    add(AniLibertyRelease(id, title, alias, poster))
                }
            }
        }
    }

    suspend fun search(query: String): List<AniLibertyRelease> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        val url = "https://aniliberty.top/api/v1/app/search/releases?query=" +
            URLEncoder.encode(q, "UTF-8")
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("AniLiberty API: HTTP ${response.code}")
            val responseBody = response.body?.string() ?: throw IOException("Пустой ответ AniLiberty")
            if (responseBody.length > 1_000_000) throw IOException("Слишком большой ответ API")
            val array = JSONArray(responseBody)
            buildList {
                for (i in 0 until minOf(array.length(), 30)) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optInt("id")
                    if (id <= 0) continue
                    val name = obj.optJSONObject("name")
                    val title = name?.optString("main").orEmpty().ifBlank {
                        obj.optString("name", "Релиз #$id")
                    }
                    val preview = obj.optJSONObject("poster")?.optString("preview").orEmpty()
                    val poster = if (preview.startsWith("/storage/")) "https://aniliberty.top" + preview else preview.takeIf { it.startsWith("https://") }
                    add(AniLibertyRelease(id, title.take(160), obj.optString("alias").takeIf { it.matches(Regex("[a-zA-Z0-9_-]+")) }, poster))
                }
            }
        }
    }
}

@Composable
fun AniLibertySearchScreen(onBack: () -> Unit, onPlayBuiltIn: (StreamItem) -> Unit, initialRelease: AniLibertyRelease? = null, onReleaseSelected: (AniLibertyRelease) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf<List<AniLibertyRelease>>(emptyList()) }
    var selected by remember { mutableStateOf<AniLibertyRelease?>(null) }
    var episodes by remember { mutableStateOf<List<LibertyEpisode>>(emptyList()) }
    var quality by remember { mutableStateOf("Авто") }
    var loading by remember { mutableStateOf(false) }
    var playLoading by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialRelease?.id) {
        if (initialRelease != null) {
            loading = true
            error = null
            try {
                episodes = AniLibertyEpisodesApi.releaseEpisodes(initialRelease.id)
                selected = initialRelease
            } catch (e: Exception) {
                error = e.message ?: "Не удалось открыть релиз"
            } finally { loading = false }
        }
    }

    LaunchedEffect(query) {
        matches = emptyList()
        error = null
        if (query.trim().length < 2) return@LaunchedEffect
        delay(450)
        loading = true
        try {
            matches = AniLibertyApi.search(query)
        } catch (e: Exception) {
            error = e.message ?: "Ошибка поиска AniLiberty"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = {
            if (selected != null) selected = null else onBack()
        }) { Text("← Назад") }
        Text("AniLiberty — серии и просмотр", color = Color.White,
            style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        if (selected == null) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("Название аниме") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(selected!!.title, color = Color.White,
                style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Качество видео", color = Color.LightGray)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val choices = listOf("Авто", "1080p", "720p", "480p")
                choices.forEachIndexed { index, q ->
                    SegmentedButton(
                        selected = quality == q,
                        onClick = { quality = q },
                        shape = SegmentedButtonDefaults.itemShape(index, choices.size)
                    ) { Text(q) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Серия запускается сразу в плеере, выбранном в настройках.",
                color = Color.LightGray)
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(8.dp))
        if (selected == null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(matches, key = { it.id }) { release ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(release.title, style = MaterialTheme.typography.titleMedium)
                            Button(enabled = !loading, onClick = {
                                scope.launch {
                                    loading = true
                                    error = null
                                    try {
                                        episodes = AniLibertyEpisodesApi.releaseEpisodes(release.id)
                                        selected = release
                                        onReleaseSelected(release)
                                    } catch (e: Exception) {
                                        error = e.message ?: "Не удалось получить серии"
                                    } finally { loading = false }
                                }
                            }) { Text("Выбрать аниме") }
                        }
                    }
                }
            }
        } else {
            if (episodes.isEmpty() && !loading) {
                Text("У этого релиза нет доступных серий.", color = Color.LightGray)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(episodes, key = { it.id }) { episode ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Серия " + episode.number +
                                    (if (episode.title.isNotBlank()) " · " + episode.title else ""),
                                style = MaterialTheme.typography.titleMedium
                            )
                            val saved = remember(episode.id) {
                                context.getSharedPreferences("playback_positions", Context.MODE_PRIVATE)
                                    .getLong("position_aniliberty-${episode.id}", 0L)
                            }
                            if (saved > 0L) Text(
                                "Последняя позиция: ${saved / 60000} мин ${saved / 1000 % 60} сек (встроенный плеер)",
                                color = Color.LightGray, style = MaterialTheme.typography.bodySmall
                            )
                            Button(enabled = playLoading == null, onClick = {
                                scope.launch {
                                    playLoading = episode.id
                                    error = null
                                    try {
                                        // Refresh HLS URLs just before playback. Query parameters may change.
                                        val fresh = try {
                                            AniLibertyEpisodesApi.currentEpisode(episode.id)
                                        } catch (_: IOException) {
                                            AniLibertyEpisodesApi.releaseEpisodes(selected!!.id)
                                                .firstOrNull { it.id == episode.id }
                                                ?: throw IOException("Серия больше не доступна")
                                        }
                                        val stream = fresh.stream(quality)
                                            ?: throw IOException("Для выбранного качества нет HLS-ссылки")
                                        val item = StreamItem(
                                            "aniliberty-" + episode.id,
                                            selected!!.title + " · серия " + episode.number +
                                                " (" + stream.first + ")",
                                            stream.second,
                                            "AniLiberty API v1"
                                        )
                                        if (!PlaybackPreferences.launch(context, item.url)) {
                                            onPlayBuiltIn(item)
                                        }
                                    } catch (e: Exception) {
                                        error = e.message ?: "Не удалось запустить плеер"
                                    } finally { playLoading = null }
                                }
                            }) {
                                Text(if (playLoading == episode.id) "Загрузка..." else "▶ Смотреть серию")
                            }
                        }
                    }
                }
            }
        }
    }
}
