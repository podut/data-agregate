package com.podut.dataagregate.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.podut.dataagregate.core.domain.model.TechNews
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

// ─── Constants & Utils ──────────────────────────────────────────────────────

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

// ─── Components ─────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier) {
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
fun ArticleRow(
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
