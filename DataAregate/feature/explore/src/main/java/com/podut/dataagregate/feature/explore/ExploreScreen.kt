package com.podut.dataagregate.feature.explore

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.podut.dataagregate.core.domain.model.TechNews
import com.podut.dataagregate.core.ui.components.ShimmerBox
import com.podut.dataagregate.core.ui.Sora
import com.podut.dataagregate.core.ui.R as CoreR
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

@Composable
fun ExploreScreen(
    onCategoryClick: (String) -> Unit = {},
    viewModel: ExploreViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.init()
    }
    
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F17))) {
        Image(
            painter = painterResource(id = CoreR.drawable.bg_explore),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Explore",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { viewModel.toggleSchedulerDialog(true) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("My Interests", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            
            when {
                uiState.categories.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No interests yet. Add them in Settings.", color = Color.Gray)
                    }
                else ->
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.categories) { cat ->
                            FeaturedCategoryCard(
                                title = cat,
                                article = uiState.featuredByCategory[cat],
                                color = getCatColor(cat),
                                onClick = { onCategoryClick(cat) }
                            )
                        }
                    }
            }
        }

        if (uiState.showSchedulerDialog) {
            SyncSchedulerDialog(
                currentInterval = uiState.currentInterval,
                onDismiss = { viewModel.toggleSchedulerDialog(false) },
                onConfirm = { viewModel.updateScheduler(it) }
            )
        }
    }
}

@Composable
fun SyncSchedulerDialog(
    currentInterval: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var interval by remember { mutableStateOf(currentInterval.toFloat()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync Interval", color = Color.White) },
        text = {
            Column {
                Text(
                    "Every ${interval.toInt()} hours",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Slider(
                    value = interval,
                    onValueChange = { interval = it },
                    valueRange = 1f..24f,
                    steps = 23,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF8A2BE2),
                        activeTrackColor = Color(0xFF8A2BE2)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(interval.toInt()) }) {
                Text("SAVE", color = Color(0xFF8A2BE2), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1A1A24),
        shape = RoundedCornerShape(24.dp)
    )
}

fun getCatColor(cat: String): Color {
    val palette = listOf(
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF2196F3),
        Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF00BCD4)
    )
    return palette[cat.hashCode().absoluteValue % palette.size]
}

@Composable
fun FeaturedCategoryCard(
    title: String,
    article: TechNews?,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image or Gradient
            if (article != null && !article.image.isNullOrBlank()) {
                Box(Modifier.fillMaxSize()) {
                    ShimmerBox(Modifier.fillMaxSize())
                    AsyncImage(
                        model = article.image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0.15f)),
                                start = Offset(0f, 0f),
                                end = Offset(1000f, 1000f)
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-40).dp)
                            .background(color.copy(alpha = 0.2f), RoundedCornerShape(75.dp))
                    )
                }
            }

            // Overlay Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    color = color.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = title.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        letterSpacing = 0.5.sp
                    )
                }
                
                Spacer(Modifier.height(12.dp))

                Text(
                    text = article?.title ?: "Discover the latest in $title",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (article != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(color.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = article.source.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${article.source} • ${article.publish_date?.split("T")?.firstOrNull() ?: ""}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
