package com.podut.dataagregate.feature.saved

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.podut.dataagregate.core.ui.components.ArticleRow
import com.podut.dataagregate.core.ui.Sora
import com.podut.dataagregate.core.ui.appBg
import com.podut.dataagregate.core.ui.appBgImageAlpha
import com.podut.dataagregate.core.ui.R as CoreR


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    onArticleClick: (url: String, title: String) -> Unit = { _, _ -> },
    viewModel: SavedViewModel = hiltViewModel()
) {
    val s by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(appBg())) {
        Image(
            painter = painterResource(id = CoreR.drawable.bg_saved),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = appBgImageAlpha()
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                "Saved Articles",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (s.articles.isNotEmpty()) {
                                Text(
                                    "${s.articles.size} items stored",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (s.articles.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.BookmarkRemove, null,
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp))
                            }
                        }
                        Text("No saved articles yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground)
                        Text("Articles you bookmark will appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 16.dp,
                        bottom = 100.dp, start = 16.dp, end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(s.articles, key = { it.link }) { article ->
                        ArticleRow(
                            article = article,
                            isSaved = true,
                            onClick = { onArticleClick(article.link, article.title) },
                            onSave = { viewModel.remove(article.link) }
                        )
                    }
                }
            }
        }
    }
}
