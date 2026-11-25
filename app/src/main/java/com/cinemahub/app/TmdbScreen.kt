package com.cinemahub.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class TmdbItem(
    val id: Int,
    val title: String,
    val date: String,
    val overview: String,
    val poster: String?,
    val rating: Double,
    val kind: String
)

private fun fetchTmdb(apiKey: String, query: String, searchMode: Boolean): List<TmdbItem> {
    val path = if (searchMode && query.isNotBlank()) "search/multi" else "trending/all/week"
    val request = buildString {
        append("https://api.themoviedb.org/3/")
        append(path)
        append("?api_key=")
        append(URLEncoder.encode(apiKey, "UTF-8"))
        append("&language=ru-RU&include_adult=false")
        if (searchMode && query.isNotBlank()) {
            append("&query=")
            append(URLEncoder.encode(query, "UTF-8"))
        }
    }
    val connection = URL(request).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        val status = connection.responseCode
        if (status !in 200..299) {
            throw IllegalStateException(
                when (status) {
                    401 -> "Неверный API Key TMDB (v3). Проверь ключ."
                    429 -> "TMDB временно ограничил число запросов."
                    else -> "Ошибка TMDB: HTTP $status"
                }
            )
        }
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response).getJSONArray("results")
        return buildList {
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                val kind = item.optString("media_type", "movie")
                if (kind == "person") continue
                add(TmdbItem(
                    id = item.optInt("id"),
                    title = item.optString("title").ifBlank { item.optString("name", "Без названия") },
                    date = item.optString("release_date").ifBlank { item.optString("first_air_date") },
                    overview = item.optString("overview"),
                    poster = item.optString("poster_path").takeIf { it.startsWith("/") },
                    rating = item.optDouble("vote_average", 0.0),
                    kind = kind
                ))
            }
        }
    } finally {
        connection.disconnect()
    }
}

@Composable
fun TmdbScreen(searchMode: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("tmdb_settings", Context.MODE_PRIVATE) }
    var savedKey by remember { mutableStateOf(prefs.getString("api_key_v3", "") ?: "") }
    var enteredKey by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<TmdbItem?>(null) }

    LaunchedEffect(savedKey, query, searchMode) {
        if (savedKey.isBlank()) return@LaunchedEffect
        if (searchMode && query.isBlank()) {
            items = emptyList()
            loading = false
            error = null
            return@LaunchedEffect
        }
        if (searchMode) delay(400)
        loading = true
        error = null
        try {
            items = withContext(Dispatchers.IO) { fetchTmdb(savedKey, query, searchMode) }
        } catch (e: Exception) {
            error = e.message ?: "Не удалось подключиться к TMDB"
            items = emptyList()
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(if (searchMode) "Поиск TMDB" else "Каталог TMDB", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        if (savedKey.isBlank()) {
            Text("Для подключения введи свой TMDB API Key (v3). Он сохраняется только на этом устройстве.", color = Color.LightGray)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = enteredKey, onValueChange = { enteredKey = it.trim() },
                label = { Text("TMDB API Key (v3)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val key = enteredKey.trim()
                if (key.isNotBlank()) {
                    prefs.edit().putString("api_key_v3", key).apply()
                    savedKey = key
                    enteredKey = ""
                }
            }, enabled = enteredKey.isNotBlank()) { Text("Подключить TMDB") }
        } else {
            if (selected != null) {
                val film = selected!!
                TextButton(onClick = { selected = null }) { Text("← Назад") }
                Text(film.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = film.poster?.let { "https://image.tmdb.org/t/p/w500$it" },
                    contentDescription = film.title,
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                )
                Text("${film.date.take(4)} · ${if (film.kind == "tv") "Сериал" else "Фильм"} · TMDB ${"%.1f".format(film.rating)}", color = Color.LightGray)
                Spacer(Modifier.height(12.dp))
                Text(film.overview.ifBlank { "Описание пока недоступно" }, color = Color.White)
            } else {
                if (searchMode) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Название фильма или сериала") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                TextButton(onClick = {
                    prefs.edit().remove("api_key_v3").apply()
                    savedKey = ""
                    items = emptyList()
                }) { Text("Сменить API-ключ") }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!loading && searchMode && query.isBlank()) Text("Введи название для поиска", color = Color.LightGray)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { "${it.kind}-${it.id}" }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selected = item },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color.DarkGray) {
                                AsyncImage(
                                    model = item.poster?.let { "https://image.tmdb.org/t/p/w185$it" },
                                    contentDescription = "Постер ${item.title}",
                                    modifier = Modifier.width(86.dp).height(126.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(item.date.take(4), color = Color.LightGray)
                                Text(if (item.kind == "tv") "Сериал" else "Фильм", color = Color(0xFF9B8CFF))
                                Text("TMDB: ${"%.1f".format(item.rating)}", color = Color.LightGray)
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(14.dp))
                        Text("This product uses the TMDB API but is not endorsed or certified by TMDB.", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
