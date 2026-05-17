package com.podut.dataagregate.feature.news_feed

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.podut.dataagregate.core.domain.model.TechNews
import com.podut.dataagregate.core.ui.components.ArticleRow
import com.podut.dataagregate.core.ui.components.ShimmerBox
import com.podut.dataagregate.core.ui.Sora
import com.podut.dataagregate.core.ui.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

// ─── Shimmer placeholder ──────────────────────────────────────────────────
@Composable
private fun ShimmerBox(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.04f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f),
                ),
                start = Offset(translateX * 600f, 0f),
                end   = Offset(translateX * 600f + 600f, 0f)
            )
        )
    )
}

// ─── Fixed semantic colors ─────────────────────────────────────────────────
private val ScoreGreen = Color(0xFF22C55E)

private val HeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF5B21B6), Color(0xFF1D4ED8)),
    start  = Offset(0f, 0f), end = Offset(900f, 200f)
)
private val OverlayGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF4C1D95).copy(alpha = 0.97f), Color.Transparent),
    startX = 0f, endX = 580f
)

private fun categoryColor(cat: String) = when (cat.lowercase()) {
    "ai"      -> Color(0xFF06B6D4)
    "tools"   -> Color(0xFF22C55E)
    "dev"     -> Color(0xFFF59E0B)
    "startups" -> Color(0xFF8B5CF6)
    else      -> Color(0xFF3B82F6)
}

private fun sourceColor(src: String): Color {
    val palette = listOf(
        Color(0xFF4F46E5), Color(0xFF0EA5E9), Color(0xFF16A34A),
        Color(0xFFD97706), Color(0xFFDB2777), Color(0xFF7C3AED)
    )
    return palette[src.hashCode().absoluteValue % palette.size]
}

private fun String.timeAgo(): String = try {
    val sdf  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = sdf.parse(this) ?: return ""
    val diff = System.currentTimeMillis() - date.time
    when {
        diff < 60_000     -> "now"
        diff < 3_600_000  -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else              -> "${diff / 86_400_000}d"
    }
} catch (_: Exception) { "" }

/**
 * Returnează un tag scurt pentru când a fost publicat articolul: TODAY / YESTERDAY / null.
 * Calculat pe ziua calendaristică, nu pe diferența brută.
 */
private fun String.dayBucket(): String? = try {
    val sdf  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = sdf.parse(this) ?: return null
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    cal.time = date
    val articleDay = Calendar.getInstance().apply {
        timeInMillis = cal.timeInMillis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val diffDays = ((today.timeInMillis - articleDay.timeInMillis) / 86_400_000L).toInt()
    when (diffDays) {
        0    -> "TODAY"
        1    -> "YESTERDAY"
        else -> null
    }
} catch (_: Exception) { null }

private val CATEGORIES = listOf("All", "AI", "Dev", "Tools", "Startups")

// ─── Screen ────────────────────────────────────────────────────────────────
@Composable
fun NewsFeedScreen(
    onArticleClick: (url: String, title: String) -> Unit = { _, _ -> },
    viewModel: NewsFeedViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initProfile()
    }

    val s  by viewModel.uiState.collectAsState()
    val bg = Color(0xFF0F0F17)

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Image(
            painter            = painterResource(R.drawable.bg_home),
            contentDescription = null,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop,
            alpha              = 0.4f
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item { DigestHeader(lastSyncTime = s.lastSyncTime, onRefresh = { viewModel.refreshFeed() }) }
            item {
                SearchBar(
                    query = s.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) }
                )
            }
            item {
                when {
                    s.isLoading && s.heroArticles.isEmpty() -> LoadingHero()
                    s.error != null && s.heroArticles.isEmpty() -> ErrorHero(
                        message = s.error ?: "",
                        onRetry = { viewModel.refreshFeed() }
                    )
                    s.heroArticles.isNotEmpty() -> HeroCarousel(
                        articles   = s.heroArticles,
                        savedLinks = s.savedLinks,
                        onClick    = onArticleClick,
                        onSave     = { viewModel.toggleSave(it) }
                    )
                }
            }
            if (s.articles.isNotEmpty()) {
                item { SectionHeader("Latest News") }
            }
            items(s.articles, key = { it.link }) { article ->
                ArticleRow(
                    article = article,
                    isSaved = article.link in s.savedLinks,
                    onClick = { onArticleClick(article.link, article.title) },
                    onSave  = { viewModel.toggleSave(article.link) }
                )
            }
            if (s.hasMore) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                        OutlinedButton(
                            onClick = { viewModel.loadMore() },
                            border  = BorderStroke(1.dp, Color(0xFF8A2BE2)),
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Load More") }
                    }
                }
            }
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────
@Composable
private fun DigestHeader(lastSyncTime: String?, onRefresh: () -> Unit) {
    val onBg = Color.White
    val textSec = Color.LightGray

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = onBg)
        }
        Column(Modifier.weight(1f)) {
            Text("Tech Digest", color = onBg, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(
                if (lastSyncTime != null) "Last sync: $lastSyncTime" else "Your daily dose of tech ⚡",
                color = textSec, fontSize = 14.sp
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = onBg)
        }
        Spacer(Modifier.width(8.dp))
        Image(
            painter = rememberAsyncImagePainter("https://api.dicebear.com/7.x/avataaars/svg?seed=Felix"),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, Color.LightGray, CircleShape)
        )
    }
}

// ─── Search Bar ───────────────────────────────────────────────────────────
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value         = query,
        onValueChange = onQueryChange,
        modifier      = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder   = { Text("Caută articole, surse, categorii...", color = Color.Gray, fontSize = 14.sp) },
        leadingIcon   = { Icon(Icons.Default.Search, null, tint = Color.LightGray) },
        trailingIcon  = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, null, tint = Color.LightGray)
                }
            }
        },
        singleLine    = true,
        shape         = RoundedCornerShape(16.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor   = Color.White.copy(alpha = 0.12f),
            unfocusedBorderColor    = Color.Transparent,
            focusedBorderColor      = Color(0xFF8A2BE2),
            unfocusedTextColor      = Color.White,
            focusedTextColor        = Color.White,
            cursorColor             = Color(0xFF8A2BE2)
        )
    )
}

// ─── Hero Carousel ────────────────────────────────────────────────────────
@Composable
private fun HeroCarousel(
    articles: List<TechNews>,
    savedLinks: Set<String>,
    onClick: (String, String) -> Unit,
    onSave: (String) -> Unit
) {
    val pager  = rememberPagerState(pageCount = { articles.size })

    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalPager(
            state          = pager,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing    = 12.dp
        ) { i ->
            HeroCard(
                article = articles[i],
                isSaved = articles[i].link in savedLinks,
                onClick = { onClick(articles[i].link, articles[i].title) },
                onSave  = { onSave(articles[i].link) }
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
            repeat(articles.size) { i ->
                val sel = pager.currentPage == i
                val w by animateDpAsState(if (sel) 20.dp else 6.dp, label = "dot")
                Box(
                    Modifier.padding(horizontal = 2.dp).height(6.dp).width(w)
                        .background(if (sel) Color(0xFF8A2BE2) else Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    article: TechNews,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth().height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(HeroGradient)
            .clickable { onClick() }
    ) {
        if (!article.image.isNullOrBlank()) {
            Box(
                modifier = Modifier.size(140.dp).align(Alignment.CenterEnd)
                    .padding(end = 12.dp).clip(RoundedCornerShape(18.dp))
            ) {
                ShimmerBox(Modifier.fillMaxSize())
                AsyncImage(
                    model = article.image, contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(Modifier.fillMaxSize().background(OverlayGradient))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .fillMaxWidth(if (article.image.isNullOrBlank()) 1f else 0.65f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(12.dp))
                        Text("TOP STORY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    article.publish_date?.dayBucket()?.let { bucket ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (bucket == "TODAY") Color(0xFF22C55E).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                bucket,
                                color = if (bucket == "TODAY") Color(0xFF86EFAC) else Color.White.copy(alpha = 0.9f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = article.title.replace(Regex("<[^>]*>"), ""),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 25.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.summary.replace(Regex("<[^>]*>"), ""),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourceDot(article.source, 18)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${article.source} • ${article.publish_date?.timeAgo() ?: ""}",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onSave, modifier = Modifier.size(30.dp)) {
                    Icon(
                        if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        null, tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String) {
    val purple = Color(0xFF8A2BE2)
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).background(purple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("For you", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Text("See all", color = Color(0xFFB19CD9), fontSize = 14.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFB19CD9), modifier = Modifier.size(20.dp))
    }
}

// ─── Article Row ──────────────────────────────────────────────────────────
@Composable
internal fun ArticleRow(
    article: TechNews,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSave: () -> Unit
) {
    val cardBg = Color(0xFF1E1E2E).copy(alpha = 0.7f)
    val onBg = Color.White
    val textSec = Color.LightGray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f))) {
                if (!article.image.isNullOrBlank()) {
                    ShimmerBox(Modifier.fillMaxSize())
                    AsyncImage(model = article.image, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(article.source.take(1).uppercase(), color = Color.White.copy(alpha = 0.5f), fontSize = 24.sp)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        article.category.uppercase(),
                        color = categoryColor(article.category),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    article.publish_date?.dayBucket()?.let { bucket ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (bucket == "TODAY") Color(0xFF22C55E).copy(alpha = 0.18f)
                                    else Color.White.copy(alpha = 0.10f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                bucket,
                                color = if (bucket == "TODAY") Color(0xFF4ADE80) else Color.LightGray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    
                    // Compact Score
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${article.score ?: 0}",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.title.replace(Regex("<[^>]*>"), ""),
                    color = onBg,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.summary.replace(Regex("<[^>]*>"), ""),
                    color = textSec,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        article.source,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(Color.White.copy(alpha = 0.4f), CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        article.publish_date?.timeAgo() ?: "",
                        color = textSec.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSave, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            null, tint = Color.White, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────
@Composable
private fun ScoreBadge(score: Int) {
    Box(
        Modifier.background(ScoreGreen, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        Alignment.Center
    ) {
        Text("$score", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun SourceDot(source: String, size: Int) {
    Box(Modifier.size(size.dp).background(sourceColor(source), CircleShape), Alignment.Center) {
        Text(source.take(1).uppercase(), color = Color.White,
            fontWeight = FontWeight.Bold, fontSize = (size * 0.52f).sp)
    }
}

@Composable
private fun LoadingHero() {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = Color(0xFF8A2BE2))
            Text("AI curates your digest…", color = Color.LightGray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ErrorHero(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, color = Color(0xFFEF4444), fontSize = 13.sp)
            Button(onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A2BE2))
            ) { Text("Retry") }
        }
    }
}
