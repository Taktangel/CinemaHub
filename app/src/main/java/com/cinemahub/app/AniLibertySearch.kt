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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class AniLibertyRelease(val id: Int, val title: String, val alias: String?)

private object AniLibertyApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

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
                    add(AniLibertyRelease(id, title.take(160), obj.optString("alias").takeIf { it.matches(Regex("[a-zA-Z0-9_-]+")) }))
                }
            }
        }
    }
}

@Composable
fun AniLibertySearchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf<List<AniLibertyRelease>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        matches = emptyList()
        error = null
        if (query.trim().length < 2) return@LaunchedEffect
        delay(500)
        loading = true
        try {
            matches = AniLibertyApi.search(query)
        } catch (e: Exception) {
            error = e.message ?: "Не удалось получить данные AniLiberty"
        } finally {
            loading = false
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← К источникам") }
        Text("Поиск AniLiberty", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Публичный API v1 — поиск названий аниме. Результаты открываются на сайте AniLiberty; воспроизведение внутри CinemaHub ещё не подключено.",
            color = Color.LightGray
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Название аниме") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!loading && query.trim().length > 1 && error == null && matches.isEmpty()) {
            Text("Ничего не найдено или сервер вернул пустой список.", color = Color.LightGray)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches, key = { it.id }) { release ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(release.title, style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = {
                            openProviderWebsite(
                                context,
                                "https://aniliberty.top/anime/releases/release/${release.id}"
                            )
                        }) { Text("Открыть на AniLiberty") }
                    }
                }
            }
        }
    }
}
