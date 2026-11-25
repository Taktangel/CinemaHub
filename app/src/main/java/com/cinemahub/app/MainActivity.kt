package com.cinemahub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinemahub.app.ui.theme.CinemaHubTheme

private data class Film(val title: String, val year: String, val genre: String, val emoji: String, val tint: Color)
private val sampleFilms = listOf(
    Film("Интерстеллар", "2014", "Фантастика", "🌌", Color(0xFF263D65)),
    Film("Дюна: Часть вторая", "2024", "Фантастика", "🏜️", Color(0xFF705138)),
    Film("Начало", "2010", "Триллер", "🌀", Color(0xFF40596A)),
    Film("Паразиты", "2019", "Драма", "🏠", Color(0xFF4A5747)),
    Film("Грань будущего", "2014", "Фантастика", "⚔️", Color(0xFF4A505A)),
    Film("Бегущий по лезвию 2049", "2017", "Фантастика", "🌃", Color(0xFF68453D))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CinemaHubTheme { CinemaApp() } }
    }
}

@Composable
private fun CinemaApp() {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedFilm by remember { mutableStateOf<Film?>(null) }
    val filtered = sampleFilms.filter { it.title.contains(query, ignoreCase = true) }
    var playing by remember { mutableStateOf<StreamItem?>(null) }
    var activeMovie by remember { mutableStateOf<LibraryMovie?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Surface(color = Color(0xFF0D1016), modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("C", color = Color(0xFF9B8CFF), fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(5.dp))
                    Text("CINEMA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(" HUB", color = Color(0xFF9B8CFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Home, contentDescription = "Главная", tint = if (selectedTab == 0) Color(0xFF9B8CFF) else Color.LightGray, modifier = Modifier.clickable { selectedTab = 0 })
                    Icon(Icons.Default.Search, contentDescription = "Поиск", tint = if (selectedTab == 1) Color(0xFF9B8CFF) else Color.LightGray, modifier = Modifier.clickable { selectedTab = 1 })
                    Icon(Icons.Default.Favorite, contentDescription = "Избранное", tint = if (selectedTab == 2) Color(0xFF9B8CFF) else Color.LightGray, modifier = Modifier.clickable { selectedTab = 2 })
                    Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = if (selectedTab == 3) Color(0xFF9B8CFF) else Color.LightGray, modifier = Modifier.clickable { selectedTab = 3 })
                }
            }
            if (playing == null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
                TextButton(onClick = { selectedTab = 4; selectedFilm = null; activeMovie = null }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Источники / Плеер")
                }
            }
            }
            if (playing != null) {
                VideoPlayerScreen(stream = playing!!, onClose = { playing = null })
            } else if (selectedFilm != null) {
                FilmDetails(film = selectedFilm!!, onBack = { selectedFilm = null })
            } else when (selectedTab) {
                1 -> TmdbScreen(searchMode = true, onOpenSources = { activeMovie = it; selectedTab = 4 })
                2 -> PersonalLibraryScreen(onOpenSources = { activeMovie = it; selectedTab = 4 })
                3 -> CatalogAdapterSettingsScreen(onOpenProviders = { selectedTab = 5 })
                5 -> ProviderDirectoryScreen(onBack = { selectedTab = 3 }, onOpenAniLiberty = { selectedTab = 6 })
                6 -> AniLibertySearchScreen(onBack = { selectedTab = 5 })
                4 -> SourcesScreen(onPlay = { playing = it }, onBack = { selectedTab = if (activeMovie != null) 2 else 0; activeMovie = null }, movie = activeMovie, onPlayed = { MediaLibrary.recordPlayed(context, it) })
                else -> TmdbScreen(searchMode = false, onOpenSources = { activeMovie = it; selectedTab = 4 })
            }
        }
    }
}

@Composable
private fun HomePage(films: List<Film>, onFilm: (Film) -> Unit, query: String, onQuery: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(bottom = 20.dp)) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("ТВОЯ МЕДИАТЕКА", color = Color(0xFF9B8CFF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(5.dp))
            Text("Что будем смотреть?", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            SearchBar(query, onQuery, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            FeaturedCard(onClick = { onFilm(films[1]) })
            Spacer(Modifier.height(22.dp))
            SectionHeading("Популярное", "Подборка для старта")
        }
        Spacer(Modifier.height(12.dp))
        FilmRow(films, onFilm)
        Spacer(Modifier.height(18.dp))
        Column(Modifier.padding(horizontal = 20.dp)) { SectionHeading("Продолжить знакомство", "Демо-каталог") }
        Spacer(Modifier.height(10.dp))
        FilmRow(films.reversed(), onFilm)
    }
}

@Composable
private fun SearchPage(query: String, onQuery: (String) -> Unit, films: List<Film>, onFilm: (Film) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Поиск", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        SearchBar(query, onQuery, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        if (query.isBlank()) Text("Введи название фильма", color = Color.Gray)
        else if (films.isEmpty()) Text("Ничего не найдено в демо-каталоге", color = Color.Gray)
        else FilmRow(films, onFilm)
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.background(Color(0xFF1A202B), RoundedCornerShape(14.dp)).then(modifier).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9B8CFF))
        Spacer(Modifier.width(10.dp))
        BasicTextField(value = query, onValueChange = onQuery, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp), modifier = Modifier.fillMaxWidth(), decorationBox = { inner ->
            if (query.isEmpty()) Text("Название фильма или сериала", color = Color.Gray, fontSize = 15.sp)
            inner()
        })
    }
}

@Composable
private fun FeaturedCard(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(172.dp).background(Color(0xFF30284C), RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(20.dp)) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text("РЕКОМЕНДАЦИЯ", color = Color(0xFFC3B9FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(5.dp))
            Text("Дюна: Часть вторая", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("2024  ·  Фантастика  ·  Приключения", color = Color(0xFFD2D0DA), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text("Открыть карточку  →", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("🏜️", fontSize = 54.sp, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color(0xFF858B99), fontSize = 12.sp)
    }
}

@Composable
private fun FilmRow(films: List<Film>, onFilm: (Film) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(films) { film -> FilmPoster(film, onClick = { onFilm(film) }) }
    }
}

@Composable
private fun FilmPoster(film: Film, onClick: () -> Unit) {
    Column(Modifier.width(132.dp).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(174.dp).background(film.tint, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Text(film.emoji, fontSize = 48.sp)
            Box(Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color(0xAA11131A), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 4.dp)) {
                Text(film.year, color = Color.White, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(film.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(film.genre, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun FilmDetails(film: Film, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("← Назад", color = Color(0xFF9B8CFF), modifier = Modifier.clickable(onClick = onBack))
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(230.dp).background(film.tint, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) { Text(film.emoji, fontSize = 84.sp) }
        Spacer(Modifier.height(20.dp))
        Text(film.title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("${film.year}  ·  ${film.genre}", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text("Демо-карточка. На следующем этапе подключим реальные данные: описание, постер, рейтинг, подборки и список доступных источников.", color = Color(0xFFCFD2DA), fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B6BE8))) { Text("Источники — скоро") }
    }
}

@Composable
private fun EmptyPage(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(message, color = Color.Gray, fontSize = 14.sp)
    }
}
