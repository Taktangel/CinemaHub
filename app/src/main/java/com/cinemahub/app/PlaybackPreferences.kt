package com.cinemahub.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class PreferredPlayer(val label: String, val packageName: String)

object PlaybackPreferences {
    private const val PREFS = "cinemahub_playback_preferences"
    private const val KEY = "player_package"
    const val ASK = "ask"
    const val BUILTIN = "builtin"

    val candidates = listOf(
        PreferredPlayer("VLC", "org.videolan.vlc"),
        PreferredPlayer("Just Player", "com.brouken.player"),
        PreferredPlayer("MX Player", "com.mxtech.videoplayer.ad"),
        PreferredPlayer("MX Player Pro", "com.mxtech.videoplayer.pro"),
        PreferredPlayer("mpv-android", "is.xyz.mpv")
    )

    fun selected(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, ASK) ?: ASK

    fun setSelected(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, value).apply()
    }

    fun isInstalled(context: Context, packageName: String): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun playerIntent(url: String): Intent {
        val uri = Uri.parse(url)
        val path = uri.path.orEmpty().lowercase()
        val type = when {
            path.endsWith(".m3u8") -> "application/x-mpegURL"
            path.endsWith(".mpd") -> "application/dash+xml"
            else -> "video/*"
        }
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, type)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Returns true for a launch in an external application. False means built-in. */
    fun launch(context: Context, url: String): Boolean {
        val choice = selected(context)
        if (choice == BUILTIN) return false
        val intent = playerIntent(url)
        if (choice == ASK) {
            context.startActivity(Intent.createChooser(intent, "Выбери видеоплеер").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return true
        }
        try {
            context.startActivity(intent.apply { setPackage(choice) })
            return true
        } catch (_: ActivityNotFoundException) {
            // A removed or incompatible player should never leave playback unusable.
            context.startActivity(Intent.createChooser(playerIntent(url), "Плеер недоступен — выбери другой").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return true
        }
    }
}

@Composable
fun PlaybackSettingsSection() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(PlaybackPreferences.selected(context)) }
    val options = listOf(
        PreferredPlayer("Спрашивать каждый раз", PlaybackPreferences.ASK),
        PreferredPlayer("Встроенный плеер", PlaybackPreferences.BUILTIN)
    ) + PlaybackPreferences.candidates
    Text("Плеер по умолчанию", color = Color.White, style = MaterialTheme.typography.titleMedium)
    Text("Выбранный плеер открывается сразу для всех видео CinemaHub.", color = Color.LightGray)
    options.forEach { option ->
        val installed = option.packageName == PlaybackPreferences.ASK ||
            option.packageName == PlaybackPreferences.BUILTIN ||
            PlaybackPreferences.isInstalled(context, option.packageName)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(option.label, color = if (installed) Color.White else Color.LightGray)
                if (!installed) Text("Не установлен", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            RadioButton(
                selected = selected == option.packageName,
                enabled = installed,
                onClick = {
                    selected = option.packageName
                    PlaybackPreferences.setSelected(context, selected)
                }
            )
        }
    }
    if (selected !in options.map { it.packageName }) {
        Text("Ранее выбранный плеер недоступен. Выбери другой.", color = MaterialTheme.colorScheme.error)
    }
}
