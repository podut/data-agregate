package com.podut.dataagregate.feature.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.podut.dataagregate.core.domain.model.FeedCategory

private val REGIONS = listOf(
    "us" to "United States", "ro" to "Romania",
    "gb" to "United Kingdom", "de" to "Germany",
    "fr" to "France", "in" to "India",
    "au" to "Australia", "ca" to "Canada"
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("OPT_IN_IS_NOT_ENABLED")
@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(s.successMsg) {
        s.successMsg?.let { snackbarHost.showSnackbar(it); viewModel.clearMsg() }
    }
    LaunchedEffect(s.error) {
        s.error?.let { snackbarHost.showSnackbar("Error: $it"); viewModel.clearMsg() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Feed Categories") },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            )
        }
    ) { padding ->
        when {
            s.isLoading && s.categories.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            s.categories.isEmpty() ->
                EmptyCategoriesState(onAdd = { showAddDialog = true })
            else ->
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top    = padding.calculateTopPadding() + 8.dp,
                        bottom = 88.dp, start = 16.dp, end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(s.categories, key = { it.name }) { cat ->
                        CategoryCard(
                            category     = cat,
                            onAddFeed    = { url -> viewModel.addFeed(cat.name, url) },
                            onToggleFeed = { url -> viewModel.toggleFeed(cat.name, url) },
                            onRemoveFeed = { url -> viewModel.removeFeed(cat.name, url) },
                            onUpdateSerp = { q, r, e -> viewModel.updateSerp(cat.name, q, r, e) },
                            onFetchSerp  = { viewModel.fetchSerp(cat.name) },
                            onDelete     = { viewModel.deleteCategory(cat.name) }
                        )
                    }
                }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onCreate  = { name, urls -> viewModel.createCategory(name, urls); showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCard(
    category: FeedCategory,
    onAddFeed: (String) -> Unit,
    onToggleFeed: (String) -> Unit,
    onRemoveFeed: (String) -> Unit,
    onUpdateSerp: (String, String, Boolean) -> Unit,
    onFetchSerp: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded       by remember { mutableStateOf(false) }
    var newFeedUrl     by remember { mutableStateOf("") }
    var showDelete     by remember { mutableStateOf(false) }
    var serpQuery      by remember(category) { mutableStateOf(category.serp.query ?: "") }
    var serpRegion     by remember(category) { mutableStateOf(category.serp.region) }
    var serpEnabled    by remember(category) { mutableStateOf(category.serp.enabled) }
    var regionExpanded by remember { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(category.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${category.feeds.size} feeds • ${category.article_count} articles",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Default.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, "Expand")
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("RSS Feeds", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary)

                    category.feeds.forEach { feed ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RssFeed, null,
                                tint = if (feed.is_active) MaterialTheme.colorScheme.primary else Color.Gray, 
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(feed.url, modifier = Modifier.weight(1f), fontSize = 12.sp,
                                maxLines = 1, 
                                color = if (feed.is_active) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.6f))
                            
                            Switch(
                                checked = feed.is_active,
                                onCheckedChange = { onToggleFeed(feed.url) },
                                modifier = Modifier.scale(0.7f)
                            )

                            IconButton(onClick = { onRemoveFeed(feed.url) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, "Remove",
                                    modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newFeedUrl, onValueChange = { newFeedUrl = it },
                            placeholder = { Text("https://feed.url/rss", fontSize = 12.sp) },
                            singleLine = true, modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newFeedUrl.isNotBlank()) { onAddFeed(newFeedUrl.trim()); newFeedUrl = "" }
                            })
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(onClick = {
                            if (newFeedUrl.isNotBlank()) { onAddFeed(newFeedUrl.trim()); newFeedUrl = "" }
                        }) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SERP / Google News", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary)
                            Text("Search Google News by query + region",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = serpEnabled, onCheckedChange = { serpEnabled = it })
                    }

                    AnimatedVisibility(visible = serpEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = serpQuery, onValueChange = { serpQuery = it },
                                label = { Text("Search query") },
                                placeholder = { Text("e.g. AI startup funding") },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) }
                            )
                            ExposedDropdownMenuBox(
                                expanded = regionExpanded, onExpandedChange = { regionExpanded = it }) {
                                OutlinedTextField(
                                    value = REGIONS.find { it.first == serpRegion }?.second ?: serpRegion,
                                    onValueChange = {}, readOnly = true,
                                    label = { Text("Region") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(regionExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Language, null, Modifier.size(18.dp)) }
                                )
                                ExposedDropdownMenu(
                                    expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                                    REGIONS.forEach { (code, label) ->
                                        DropdownMenuItem(
                                            text = { Text("$label ($code)") },
                                            onClick = { serpRegion = code; regionExpanded = false }
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { onUpdateSerp(serpQuery, serpRegion, serpEnabled) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save Config")
                                }
                                Button(
                                    onClick = { onUpdateSerp(serpQuery, serpRegion, serpEnabled); onFetchSerp() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Fetch Now")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete '${category.name}'?") },
            text  = { Text("Removes all ${category.feeds.size} feeds. Fetched articles remain.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var name    by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Category name") },
                    placeholder = { Text("e.g. AI, DevOps, Startup") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = urlText, onValueChange = { urlText = it },
                    label = { Text("RSS URLs (one per line, optional)") },
                    placeholder = { Text("https://feed.url/rss") },
                    minLines = 3, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val urls = urlText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        onCreate(name.trim(), urls)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmptyCategoriesState(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.FolderOpen, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
            Text("No categories yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Create a category and add RSS feeds",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Category")
            }
        }
    }
}
