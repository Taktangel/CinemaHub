package com.cinemahub.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.HttpDataSource
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(stream: StreamItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("playback_positions", Context.MODE_PRIVATE) }
    var error by remember { mutableStateOf<String?>(null) }
    val player = remember(stream.url) {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse(stream.url)
            val path = uri.path.orEmpty().lowercase()
            val mime = when {
                path.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
                path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
                else -> MimeTypes.VIDEO_MP4
            }
            setMediaItem(MediaItem.Builder().setUri(uri).setMimeType(mime).build())
            val position = prefs.getLong("position_${stream.id}", 0L)
            if (position > 0) seekTo(position)
            addListener(object : Player.Listener {
                override fun onPlayerError(playbackError: PlaybackException) {
                    val httpError = generateSequence(playbackError as Throwable?) { it.cause }
                        .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
                        .firstOrNull()
                    error = if (httpError != null) {
                        "Сервер видео ответил HTTP ${httpError.responseCode}. Попробуй другой источник."
                    } else {
                        "Ошибка воспроизведения: ${playbackError.errorCodeName}"
                    }
                }
            })
            prepare()
            playWhenReady = true
        }
    }
    fun savePosition() {
        val position = if (player.playbackState == Player.STATE_ENDED) 0L else player.currentPosition
        prefs.edit().putLong("position_${stream.id}", position.coerceAtLeast(0)).apply()
    }
    DisposableEffect(player) {
        onDispose {
            savePosition()
            player.release()
        }
    }
    BackHandler {
        savePosition()
        onClose()
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        TextButton(onClick = { savePosition(); onClose() }) { Text("← К источникам") }
        Text(stream.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            update = { it.player = player }
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (stream.attribution.isNotBlank()) {
            Text(stream.attribution, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
