package com.podut.dataagregate.feature.news_feed

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.podut.dataagregate.core.domain.model.TechNews
import com.podut.dataagregate.core.ui.AppError
import com.podut.dataagregate.core.ui.AppHeroGradient
import com.podut.dataagregate.core.ui.AppHeaderGradient
import com.podut.dataagregate.core.ui.AppScoreGreen
import com.podut.dataagregate.core.ui.AppScoreGreenLight
import com.podut.dataagregate.core.ui.R
import com.podut.dataagregate.core.ui.Sora
import com.podut.dataagregate.core.ui.appBgImageAlpha
import com.podut.dataagregate.core.ui.appCardBg
import com.podut.dataagregate.core.ui.appCardBorder
import com.podut.dataagregate.core.ui.appDivider
import com.podut.dataagregate.core.ui.appTextMuted
import com.podut.dataagregate.core.ui.appTextSec
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

// ─── Fixed decorative brushes (brand colours, theme-independent) ───────────────

private val OverlayGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF4C1D95).copy(alpha = 0.97f), Color.Transparent),
    startX = 0f, endX = 580f
)

private fun categoryColor(cat: String) = when (cat.lowercase()) {
    "ai"       -> Color(0xFF06B6D4)
    "tools"    -> Color(0xFF22C55E)
    "dev"      -> Color(0xFFF59E0B)
    "startups" -> Color(0xFF8B5CF6)
    else       -> Color(0xFF3B82F6)
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

private fun String.dayBucket(): String? = try {
    val sdf  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = sdf.parse(this) ?: return null
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }
    val articleDay = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }
    when (((today.timeInMillis - articleDay.timeInMillis) / 86_400_000L).toInt()) {
        0    -> "TODAY"
        1    -> "YESTERDAY"
        else -> null
    }
} catch (_: Exception) { null }

// ─── Shimmer ──────────────────────────────────────────────────────────────────

@Composable
private fun ShimmerBox(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val tx by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    Box(modifier = modifier.background(
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.04f),
            ),
            start = Offset(tx * 600f, 0f), end = Offset(tx * 600f + 600f, 0f)
        )
    ))
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    onArticleClick: (url: String, title: String) -> Unit = { _, _ -> },
    viewModel: NewsFeedViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.initProfile() }

    val s by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    // Marchează articolele vizibile ca "seen" la scroll
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visibleInfo = listState.layoutInfo.visibleItemsInfo
        val seenLinks   = visibleInfo.mapNotNull { info ->
            // offset: DigestHeader(0) + HeroCarousel(1) + SectionHeader(2) = 3
            val articleIdx = info.index - 3
            s.articles.getOrNull(articleIdx)?.link
        }.filter { it.isNotBlank() }
        if (seenLinks.isNotEmpty()) viewModel.onArticlesVisible(seenLinks)
    }

    LaunchedEffect(s.refreshMessage) {
        s.refreshMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                viewModel.clearRefreshMessage()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost   = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = Color(0xFF2A1A4A),
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(12.dp),
                    modifier       = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Image(
                painter            = painterResource(R.drawable.bg_home),
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop,
                alpha              = appBgImageAlpha()
            )

            PullToRefreshBox(
                isRefreshing = s.isRefreshing,
                onRefresh    = { viewModel.refreshFeed() },
                state        = pullState,
                modifier     = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state          = listState,
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    item {
                        DigestHeader(
                            lastSyncTime     = s.lastSyncTime,
                            newArticlesCount = s.newArticlesCount,
                            isRefreshing     = s.isRefreshing,
                            onRefresh        = { viewModel.refreshFeed() }
                        )
                    }
                    item {
                        when {
                            s.isLoading && s.heroArticles.isEmpty() -> LoadingHero()
                            s.error != null && s.heroArticles.isEmpty() ->
                                ErrorHero(message = s.error ?: "", onRetry = { viewModel.refreshFeed() })
                            s.heroArticles.isNotEmpty() -> HeroCarousel(
                                articles   = s.heroArticles,
                                savedLinks = s.savedLinks,
                                onClick    = { url, title ->
                                    viewModel.onArticleOpened(url)
                                    onArticleClick(url, title)
                                },
                                onSave = { viewModel.toggleSave(it) }
                            )
                        }
                    }

                    // ── For You section ───────────────────────────────────────
                    if (s.articles.isNotEmpty()) {
                        item { SectionHeader("For you") }
                    }

                    items(s.articles, key = { it.link }) { article ->
                        ArticleRow(
                            article = article,
                            isSaved = article.link in s.savedLinks,
                            onClick = {
                                viewModel.onArticleOpened(article.link)
                                onArticleClick(article.link, article.title)
                            },
                            onSave = { viewModel.toggleSave(article.link) }
                        )
                    }

                    // ── Trending continuation ─────────────────────────────────
                    if (s.allCaughtUp || s.trendingArticles.isNotEmpty()) {
                        item { TrendingDivider() }
                    }

                    items(s.trendingArticles, key = { "t_${it.link}" }) { article ->
                        ArticleRow(
                            article = article,
                            isSaved = article.link in s.savedLinks,
                            onClick = {
                                viewModel.onArticleOpened(article.link)
                                onArticleClick(article.link, article.title)
                            },
                            onSave = { viewModel.toggleSave(article.link) }
                        )
                    }

                    if (s.allCaughtUp && s.trendingArticles.isEmpty()) {
                        item { AllCaughtUpCard(onRefresh = { viewModel.refreshFeed() }) }
                    }

                    if (s.hasMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                OutlinedButton(
                                    onClick = { viewModel.loadMore() },
                                    border  = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                                ) { Text("Load More") }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun DigestHeader(
    lastSyncTime: String?,
    newArticlesCount: Int,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppHeaderGradient)
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Titlu + subtitle ──────────────────────────────────────────
            Column(Modifier.weight(1f)) {
                Text(
                    "DataAgregate",
                    color         = Color.White,
                    fontFamily    = Sora,
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 26.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isRefreshing) "Se actualizează..." else "Doza ta zilnică de tehnologie",
                    color    = Color.LightGray,
                    fontSize = 13.sp
                )
            }

            // ── Refresh progress spinner (no bell) ──────────────────────────
            if (isRefreshing) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

// ─── Hero Carousel ────────────────────────────────────────────────────────────

@Composable
private fun HeroCarousel(
    articles: List<TechNews>,
    savedLinks: Set<String>,
    onClick: (String, String) -> Unit,
    onSave: (String) -> Unit
) {
    val pager = rememberPagerState(pageCount = { articles.size })
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
            val primary = MaterialTheme.colorScheme.primary
            repeat(articles.size) { i ->
                val sel = pager.currentPage == i
                val w by animateDpAsState(if (sel) 20.dp else 6.dp, label = "dot")
                Box(
                    Modifier.padding(horizontal = 2.dp).height(6.dp).width(w)
                        .background(
                            if (sel) primary else Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(3.dp)
                        )
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
            .background(AppHeroGradient)
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
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            }
            Box(Modifier.fillMaxSize().background(OverlayGradient))
        }
        Column(
            modifier = Modifier
                .fillMaxSize().padding(16.dp)
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(12.dp))
                        Text("TOP STORY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    article.publish_date?.dayBucket()?.let { bucket ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (bucket == "TODAY") AppScoreGreen.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                bucket,
                                color = if (bucket == "TODAY") AppScoreGreenLight else Color.White.copy(alpha = 0.9f),
                                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = article.title.replace(Regex("<[^>]*>"), ""),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 25.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.summary.replace(Regex("<[^>]*>"), ""),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

// ─── Trending Divider ────────────────────────────────────────────────────────

@Composable
private fun TrendingDivider() {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
        Spacer(Modifier.width(12.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Whatshot, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Text("Popular in your interests", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).background(primary, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(10.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Text("See all", color = primary.copy(alpha = 0.7f), fontSize = 14.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
    }
}

// ─── Article Row ──────────────────────────────────────────────────────────────

@Composable
internal fun ArticleRow(
    article: TechNews,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSave: () -> Unit
) {
    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textMuted = appTextMuted()

    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        colors    = CardDefaults.cardColors(containerColor = appCardBg()),
        shape     = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, appCardBorder())
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (!article.image.isNullOrBlank()) {
                    ShimmerBox(Modifier.fillMaxSize())
                    AsyncImage(
                        model = article.image, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(article.source.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 24.sp)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        article.category.uppercase(),
                        color = categoryColor(article.category),
                        fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    article.publish_date?.dayBucket()?.let { bucket ->
                        val isToday = bucket == "TODAY"
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isToday) AppScoreGreen.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                bucket,
                                color = if (isToday) AppScoreGreenLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, null, tint = AppScoreGreenLight, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${article.score ?: 0}", color = AppScoreGreenLight,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.title.replace(Regex("<[^>]*>"), ""),
                    color = onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 21.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.summary.replace(Regex("<[^>]*>"), ""),
                    color = textMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        article.source,
                        color = onSurface.copy(alpha = 0.85f), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(3.dp).background(textMuted, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(article.publish_date?.timeAgo() ?: "", color = textMuted, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSave, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            null, tint = primary, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── All Caught Up ────────────────────────────────────────────────────────────

@Composable
private fun AllCaughtUpCard(onRefresh: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val primary = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    Brush.radialGradient(listOf(primary.copy(alpha = 0.3f), Color.Transparent)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint     = primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            "You're all caught up!",
            color      = MaterialTheme.colorScheme.onBackground,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "You've seen everything for now.\nCheck back later for new articles.",
            color     = appTextSec(),
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onRefresh,
            colors  = ButtonDefaults.buttonColors(containerColor = primary),
            shape   = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Refresh Feed")
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SourceDot(source: String, size: Int) {
    Box(Modifier.size(size.dp).background(sourceColor(source), CircleShape), Alignment.Center) {
        Text(
            source.take(1).uppercase(), color = Color.White,
            fontWeight = FontWeight.Bold, fontSize = (size * 0.52f).sp
        )
    }
}

@Composable
private fun LoadingHero() {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text("AI curates your digest…", color = appTextSec(), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ErrorHero(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, color = AppError, fontSize = 13.sp)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("Retry")
            }
        }
    }
}
