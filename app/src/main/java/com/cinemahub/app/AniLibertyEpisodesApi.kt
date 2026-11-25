package com.cinemahub.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class LibertyEpisode(
    val id: String,
    val number: String,
    val title: String,
    val hls480: String?,
    val hls720: String?,
    val hls1080: String?
) {
    fun stream(quality: String): Pair<String, String>? {
        val options = when (quality) {
            "1080p" -> listOf("1080p" to hls1080, "720p" to hls720, "480p" to hls480)
            "720p" -> listOf("720p" to hls720, "480p" to hls480)
            "480p" -> listOf("480p" to hls480)
            else -> listOf("1080p" to hls1080, "720p" to hls720, "480p" to hls480)
        }
        return options.firstOrNull { it.second != null && isSupportedStreamUrl(it.second!!) }
            ?.let { it.first to it.second!! }
    }
}

object AniLibertyEpisodesApi {
    private const val BASE = "https://aniliberty.top/api/v1"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(BASE + path)
            .header("Accept", "application/json").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                throw IOException("AniLiberty API: HTTP " + response.code)
            val content = response.body?.string() ?: throw IOException("Пустой ответ AniLiberty")
            if (content.length > 2_000_000) throw IOException("Ответ AniLiberty слишком большой")
            content
        }
    }

    private fun parseEpisode(obj: JSONObject): LibertyEpisode? {
        val id = obj.optString("id")
        if (id.isBlank()) return null
        val number = obj.opt("ordinal")?.toString().orEmpty()
            .removeSuffix(".0").ifBlank { "?" }
        fun validLink(field: String) = obj.optString(field).takeIf {
            it.startsWith("https://") && isSupportedStreamUrl(it)
        }
        return LibertyEpisode(
            id, number, obj.optString("name").takeUnless { it == "null" }.orEmpty(),
            validLink("hls_480"), validLink("hls_720"), validLink("hls_1080")
        )
    }

    suspend fun releaseEpisodes(releaseId: Int): List<LibertyEpisode> {
        if (releaseId <= 0) throw IOException("Неверный ID релиза")
        val root = JSONObject(get("/anime/releases/" + releaseId))
        val episodes = root.optJSONArray("episodes") ?: JSONArray()
        return buildList {
            for (i in 0 until episodes.length()) {
                val obj = episodes.optJSONObject(i) ?: continue
                parseEpisode(obj)?.let { add(it) }
            }
        }
    }

    suspend fun currentEpisode(id: String): LibertyEpisode {
        if (!id.matches(Regex("[a-zA-Z0-9_-]{10,80}")))
            throw IOException("Неверный ID серии")
        return parseEpisode(JSONObject(get("/anime/releases/episodes/" + id)))
            ?: throw IOException("Нет данных по серии")
    }
}
