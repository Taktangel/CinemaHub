package com.cinemahub.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Directory of externally maintained resources.
 * A directory entry does NOT imply an API or playable media extraction.
 */
enum class ProviderCapability { WEBSITE, DOCUMENTATION, FORUM_REFERENCE }

data class CinemaProvider(
    val id: String,
    val label: String,
    val url: String,
    val description: String,
    val capability: ProviderCapability,
    val enabledByDefault: Boolean = true
)

object CinemaProviders {
    private const val PREFERENCES = "cinema_provider_directory"
    val providers = listOf(
        CinemaProvider(
            "animego", "AnimeGO", "https://animego.me/",
            "Аниме-каталог. Открывается официальный сайт; автоматический поиск видео не реализован.",
            ProviderCapability.WEBSITE
        ),
        CinemaProvider(
            "anilibria", "AniLibria / AniLiberty", "https://anilibria.tv/",
            "Поиск аниме через актуальный публичный API AniLiberty v1; старые API v2/v3 не поддерживаются.",
            ProviderCapability.WEBSITE
        ),
        CinemaProvider(
            "hdrezka", "HdRezkaApi (Python)", "https://github.com/SuperZombi/HdRezkaApi",
            "Документация Python-библиотеки. В Android она не установлена и потоки автоматически не извлекаются.",
            ProviderCapability.DOCUMENTATION
        ),
        CinemaProvider(
            "forum902011", "4PDA — тема 902011",
            "https://4pda.to/forum/index.php?showtopic=902011&st=6680",
            "Ссылка на обсуждение. Содержимое страницы не удалось проверить (HTTP 403).",
            ProviderCapability.FORUM_REFERENCE,
            false
        ),
        CinemaProvider(
            "forum1111927", "4PDA — тема 1111927",
            "https://4pda.to/forum/index.php?showtopic=1111927",
            "Ссылка на обсуждение. Код и вложения не проверены (HTTP 403).",
            ProviderCapability.FORUM_REFERENCE,
            false
        )
    )

    fun enabled(context: Context, provider: CinemaProvider): Boolean =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(provider.id, provider.enabledByDefault)

    fun setEnabled(context: Context, provider: CinemaProvider, enabled: Boolean) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(provider.id, enabled).apply()
}

/** Open using the regular browser; does not use CinemaHub's video player. */
fun openProviderWebsite(context: Context, url: String): Boolean {
    val uri = Uri.parse(url)
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) return false
    return try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

@Composable
fun ProviderDirectoryScreen(onBack: () -> Unit, onOpenAniLiberty: () -> Unit) {
    val context = LocalContext.current
    var changed by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TextButton(onClick = onBack) { Text("← Назад к настройкам") }
        Text("Каталог сайтов и API", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "В версии 0.8 эти ресурсы открываются в браузере. Наличие сайта в списке НЕ означает, что CinemaHub получает с него видеопотоки.",
            color = Color.LightGray
        )
        Spacer(Modifier.height(12.dp))
        notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(CinemaProviders.providers, key = { it.id }) { provider ->
                val isEnabled = CinemaProviders.enabled(context, provider)
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(provider.label, style = MaterialTheme.typography.titleMedium)
                        Text(provider.description, style = MaterialTheme.typography.bodySmall)
                        val capability = when (provider.capability) {
                            ProviderCapability.WEBSITE -> "Сайт"
                            ProviderCapability.DOCUMENTATION -> "Документация API"
                            ProviderCapability.FORUM_REFERENCE -> "Справочная тема"
                        }
                        Text("Тип: $capability", style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Показывать источник")
                            Switch(checked = isEnabled, onCheckedChange = {
                                CinemaProviders.setEnabled(context, provider, it)
                                changed++
                            })
                        }
                        if (isEnabled && provider.id == "anilibria") {
                            Button(onClick = onOpenAniLiberty) { Text("Поиск через AniLiberty API") }
                        }
                        if (isEnabled) {
                            OutlinedButton(onClick = {
                                if (!openProviderWebsite(context, provider.url))
                                    notice = "Не удалось открыть ссылку в браузере."
                                else notice = null
                            }) {
                                Text("Открыть сайт / документацию")
                            }
                        }
                    }
                }
            }
        }
    }
}
