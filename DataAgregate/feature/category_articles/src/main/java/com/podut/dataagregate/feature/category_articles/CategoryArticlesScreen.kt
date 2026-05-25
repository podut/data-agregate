package com.podut.dataagregate.feature.category_articles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.podut.dataagregate.core.ui.components.ArticleRow
import com.podut.dataagregate.core.ui.AppError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryArticlesScreen(
    category: String,
    onArticleClick: (url: String, title: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryArticlesViewModel = hiltViewModel()
) {
    LaunchedEffect(category) { viewModel.load(category) }

    val s by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(category, fontWeight = FontWeight.Bold)
                        if (s.articles.isNotEmpty())
                            Text("${s.articles.size} articles", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            when {
                s.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Loading $category…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
                s.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(s.error ?: "", color = AppError, fontSize = 13.sp)
                        Button(onClick = { viewModel.load(category) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Retry") }
                    }
                }
                s.articles.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No articles yet for \"$category\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(s.articles, key = { it.link }) { article ->
                        ArticleRow(
                            article = article,
                            isSaved = article.link in s.savedLinks,
                            onClick  = { onArticleClick(article.link, article.title) },
                            onSave   = { viewModel.toggleSave(article) }
                        )
                    }
                }
            }
        }
    }
}
