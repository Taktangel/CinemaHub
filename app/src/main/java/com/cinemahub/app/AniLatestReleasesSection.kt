package com.cinemahub.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun AniLatestReleasesSection(onSelect: (AniLibertyRelease) -> Unit, onMore: () -> Unit) {
    var releases by remember { mutableStateOf<List<AniLibertyRelease>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) {
        loading = true
        error = null
        try {
            releases = AniLibertyApi.latest()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Не удалось получить свежие релизы"
        } finally { loading = false }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Новые серии AniLiberty", color = Color.White, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { refresh++ }, enabled = !loading) { Text("↻") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {
            Text("AniLiberty: $it", modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (releases.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(releases, key = { it.id }) { release ->
                    ElevatedCard(
                        modifier = Modifier.width(120.dp).clickable { onSelect(release) }
                    ) {
                        Column(Modifier.padding(6.dp)) {
                            if (release.poster != null) {
                                AsyncImage(model = release.poster, contentDescription = release.title,
                                    modifier = Modifier.fillMaxWidth().height(130.dp))
                            } else {
                                Text("🎬", modifier = Modifier.height(130.dp))
                            }
                            Text(release.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        TextButton(onClick = onMore, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text("Весь каталог и поиск AniLiberty")
        }
        HorizontalDivider()
    }
}
