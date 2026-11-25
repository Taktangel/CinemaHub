package com.cinemahub.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Small on-device library for a personal prototype; survives ordinary app updates. */
data class LibraryMovie(
    val mediaKey: String,
    val title: String,
    val year: String,
    val poster: String?,
    val kind: String
)

object MediaLibrary {
    private fun preferences(context: Context) =
        context.getSharedPreferences("cinema_media_library", Context.MODE_PRIVATE)

    fun key(kind: String, tmdbId: Int): String = "${kind}:${tmdbId}"

    fun favorites(context: Context): List<LibraryMovie> {
        val array = try {
            JSONArray(preferences(context).getString("favorites", "[]"))
        } catch (_: Exception) { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val mediaKey = obj.optString("key")
                if (mediaKey.isBlank()) continue
                add(
                    LibraryMovie(
                        mediaKey,
                        obj.optString("title"),
                        obj.optString("year"),
                        obj.optString("poster").takeIf { it.startsWith("/") },
                        obj.optString("kind", "movie")
                    )
                )
            }
        }
    }

    fun isFavorite(context: Context, mediaKey: String): Boolean =
        favorites(context).any { it.mediaKey == mediaKey }

    fun toggleFavorite(context: Context, item: LibraryMovie): Boolean {
        val old = favorites(context)
        val wasSaved = old.any { it.mediaKey == item.mediaKey }
        val updated = if (wasSaved) old.filterNot { it.mediaKey == item.mediaKey } else old + item
        val array = JSONArray()
        updated.forEach {
            array.put(JSONObject().apply {
                put("key", it.mediaKey)
                put("title", it.title)
                put("year", it.year)
                put("poster", it.poster ?: "")
                put("kind", it.kind)
            })
        }
        preferences(context).edit().putString("favorites", array.toString()).apply()
        return !wasSaved
    }

    fun videoUrl(context: Context, mediaKey: String): String =
        preferences(context).getString("url_$mediaKey", "").orEmpty()

    fun setVideoUrl(context: Context, mediaKey: String, url: String) {
        val editor = preferences(context).edit()
        if (url.isBlank()) editor.remove("url_$mediaKey")
        else editor.putString("url_$mediaKey", url)
        editor.apply()
    }

    fun recordPlayed(context: Context, item: LibraryMovie) {
        val history = favoritesFromJson(preferences(context).getString("history", "[]"))
            .filterNot { it.mediaKey == item.mediaKey }
        val array = JSONArray()
        (listOf(item) + history).take(30).forEach {
            array.put(JSONObject().apply {
                put("key", it.mediaKey)
                put("title", it.title)
                put("year", it.year)
                put("poster", it.poster ?: "")
                put("kind", it.kind)
            })
        }
        preferences(context).edit().putString("history", array.toString()).apply()
    }

    fun history(context: Context): List<LibraryMovie> =
        favoritesFromJson(preferences(context).getString("history", "[]"))

    private fun favoritesFromJson(data: String?): List<LibraryMovie> {
        val array = try { JSONArray(data ?: "[]") } catch (_: Exception) { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val v = array.optJSONObject(i) ?: continue
                val mediaKey = v.optString("key")
                if (mediaKey.isBlank()) continue
                add(LibraryMovie(mediaKey, v.optString("title"), v.optString("year"),
                    v.optString("poster").takeIf { it.startsWith("/") },
                    v.optString("kind", "movie")))
            }
        }
    }
}
