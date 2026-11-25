package com.cinemahub.app

import android.content.Context

/** Playback times are milliseconds and keyed to stable provider episode ID, never a signed HLS URL. */
object WatchProgress {
    private const val STORE = "playback_positions"

    fun key(id: String) = "position_$id"

    fun read(context: Context, id: String): Long =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .getLong(key(id), 0L).coerceAtLeast(0L)

    fun save(context: Context, id: String, positionMs: Long) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putLong(key(id), positionMs.coerceAtLeast(0L)).apply()
    }

    fun format(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0) / 1000)
        return "%02d:%02d:%02d".format(seconds / 3600, (seconds / 60) % 60, seconds % 60)
    }

    /** Accept HH:MM:SS, MM:SS, or whole seconds. */
    fun parse(text: String): Long? {
        val parts = text.trim().split(":")
        if (parts.isEmpty() || parts.size > 3) return null
        val nums = parts.map { part ->
            if (part.isEmpty() || part.length > 4 || part.any { !it.isDigit() }) return null
            part.toLongOrNull() ?: return null
        }
        if (nums.size > 1 && nums.last() > 59L) return null
        if (nums.size == 3 && nums[1] > 59L) return null
        val seconds = when (nums.size) {
            1 -> nums[0]
            2 -> nums[0] * 60 + nums[1]
            else -> nums[0] * 3600 + nums[1] * 60 + nums[2]
        }
        return if (seconds in 0..86400) seconds * 1000 else null
    }
}
