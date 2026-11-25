package com.cinemahub.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PersonalLibraryScreen(onOpenSources: (LibraryMovie) -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val favorites = remember(refresh) { MediaLibrary.favorites(context) }
    val history = remember(refresh) { MediaLibrary.history(context) }
    val imageLoader = remember(context) { TmdbNetwork.imageLoader(context) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Моя медиатека", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Избранное (${favorites.size})", color = Color.White, style = MaterialTheme.typography.titleMedium) }
            if (favorites.isEmpty()) item {
                Text("Пока пусто. Добавляй фильмы сердечком в карточке TMDB.", color = Color.LightGray)
            }
            items(favorites, key = { "favorite_${it.mediaKey}" }) { movie ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = movie.poster?.let { "https://image.tmdb.org/t/p/w185$it" },
                        imageLoader = imageLoader,
                        contentDescription = movie.title,
                        modifier = Modifier.size(width = 68.dp, height = 96.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(movie.title, color = Color.White)
                        Text(movie.year, color = Color.LightGray)
                        TextButton(onClick = { onOpenSources(movie) }) { Text("Мой источник") }
                    }
                    TextButton(onClick = {
                        MediaLibrary.toggleFavorite(context, movie)
                        refresh++
                    }) { Text("Убрать") }
                }
            }
            item { Spacer(Modifier.height(12.dp)); Text("Недавно открытые видео", color = Color.White, style = MaterialTheme.typography.titleMedium) }
            if (history.isEmpty()) item { Text("История появится после выбора видеопотока.", color = Color.LightGray) }
            items(history, key = { "history_${it.mediaKey}" }) { movie ->
                Text(movie.title, modifier = Modifier.fillMaxWidth().clickable { onOpenSources(movie) }.padding(8.dp), color = Color.White)
            }
        }
    }
}
