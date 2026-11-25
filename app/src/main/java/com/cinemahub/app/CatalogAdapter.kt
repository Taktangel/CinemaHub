package com.cinemahub.app

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * JSON-based source adapter contract.
 *
 * A configured HTTPS endpoint returns its own catalog with explicit TMDB IDs.
 * No video URL scraping, HTML parser, or access-control bypass is involved.
 */
data class CatalogEntry(
    val mediaKey: String,
    val title: String,
    val stream: StreamItem
)

interface CatalogAdapter {
    val id: String
    suspend fun load(context: Context): List<CatalogEntry>
}

object JsonCatalogAdapter : CatalogAdapter {
    override val id = "json-catalog"
    private const val PREFS = "cinema_catalog_adapters"
    private const val URL_KEY = "json_catalog_url"
    private const val ENABLED_KEY = "json_catalog_enabled"
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
    }

    fun catalogUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(URL_KEY, "").orEmpty()

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(ENABLED_KEY, false)

    fun configure(context: Context, url: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(URL_KEY, url.trim())
            .putBoolean(ENABLED_KEY, enabled)
            .apply()
    }

    private fun allowedUrl(url: String): Boolean {
        if (!isSupportedStreamUrl(url)) return false
        val uri = Uri.parse(url)
        val host = uri.host.orEmpty().lowercase()
        return host != "localhost" && host != "127.0.0.1" &&
            host != "::1" && host != "0.0.0.0" &&
            !host.endsWith(".local") && !host.endsWith(".internal")
    }

    override suspend fun load(context: Context): List<CatalogEntry> = withContext(Dispatchers.IO) {
        if (!isEnabled(context)) return@withContext emptyList()
        val url = catalogUrl(context)
        if (!allowedUrl(url)) throw IOException("Введите публичный HTTPS-адрес JSON-каталога.")
        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Каталог вернул HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Пустой каталог")
            if (body.length > 1_000_000) throw IOException("Каталог слишком большой (макс. 1 МБ)")
            val root = JSONObject(body)
            val sourceName = root.optString("name", "Мой JSON-каталог").take(70)
            val items = root.optJSONArray("items") ?: throw IOException("Нет массива items")
            val results = mutableListOf<CatalogEntry>()
            for (i in 0 until minOf(items.length(), 200)) {
                val item = items.optJSONObject(i) ?: continue
                val kind = item.optString("media_type", "movie")
                if (kind != "movie" && kind != "tv") continue
                val id = item.optInt("tmdb_id")
                if (id <= 0) continue
                val streamUrl = item.optString("url")
                if (!allowedUrl(streamUrl)) continue
                val title = item.optString("title", "Видео").take(100)
                val key = MediaLibrary.key(kind, id)
                results.add(
                    CatalogEntry(
                        key,
                        title,
                        StreamItem(
                            id = "catalog-${kind}-${id}-${i}",
                            title = title,
                            url = streamUrl,
                            source = sourceName,
                            attribution = item.optString("attribution").take(150)
                        )
                    )
                )
            }
            results
        }
    }
}

@Composable
fun CatalogAdapterSettingsScreen(onOpenProviders: () -> Unit = {}) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(JsonCatalogAdapter.catalogUrl(context)) }
    var enabled by remember { mutableStateOf(JsonCatalogAdapter.isEnabled(context)) }
    var notice by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var itemCount by remember { mutableStateOf<Int?>(null) }
    val valid = url.isBlank() || (url.startsWith("https://") && isSupportedStreamUrl(url))

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlaybackSettingsSection()
        HorizontalDivider()
        Text("Подключаемые источники", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text("JSON-каталог связывает TMDB ID с разрешёнными HTTPS-потоками. Содержимое не загружается в GitHub.", color = Color.LightGray)
        OutlinedButton(onClick = onOpenProviders) { Text("AnimeGO / AniLibria / HdRezkaApi / 4PDA") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("JSON-адаптер", color = Color.White)
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        OutlinedTextField(
            value = url,
            onValueChange = { url = it.trim() },
            label = { Text("HTTPS-ссылка на catalog.json") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = !valid
        )
        Button(enabled = valid && !isChecking, onClick = {
            JsonCatalogAdapter.configure(context, url, enabled)
            itemCount = null
            notice = "Настройки источника сохранены."
        }) { Text("Сохранить") }
        var checkToken by remember { mutableIntStateOf(0) }
        OutlinedButton(enabled = valid && url.isNotBlank() && !isChecking, onClick = {
            JsonCatalogAdapter.configure(context, url, enabled = true)
            enabled = true
            checkToken++
        }) { Text(if (isChecking) "Проверяем..." else "Проверить каталог") }
        LaunchedEffect(checkToken) {
            if (checkToken == 0) return@LaunchedEffect
            isChecking = true
            notice = null
            try {
                itemCount = JsonCatalogAdapter.load(context).size
                notice = "Каталог успешно прочитан."
            } catch (e: Exception) {
                itemCount = null
                notice = "Ошибка: ${e.message ?: "неизвестная ошибка"}"
            } finally {
                isChecking = false
            }
        }
        notice?.let { Text(it, color = if (itemCount != null) Color(0xFF8FD9B0) else Color.LightGray) }
        itemCount?.let { Text("Доступных записей: $it", color = Color.White) }
        Text("Формат JSON:", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(
            """{"name":"Моя библиотека","items":[{"tmdb_id":550,"media_type":"movie","title":"Мой фильм","url":"https://example.org/media/movie.mp4"}]}""",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
        Text("Не добавляйте в URL токены доступа и пароли. Пока поддерживается один JSON-каталог; загрузка выполняется при открытии экрана источников.", color = Color.LightGray)
    }
}
