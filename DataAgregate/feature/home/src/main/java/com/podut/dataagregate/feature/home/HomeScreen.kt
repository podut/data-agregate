package com.podut.dataagregate.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.podut.dataagregate.core.database.entity.InterestEntity
import com.podut.dataagregate.core.database.entity.RssSourceEntity
import com.podut.dataagregate.core.ui.AppScoreGreen
import com.podut.dataagregate.core.ui.FabAction
import com.podut.dataagregate.core.ui.appCardBg
import com.podut.dataagregate.core.ui.appCardBorder

private fun String.toDomain(): String = this
    .removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .split("/")
    .first()
    .ifBlank { this }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { viewModel.init() }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRssSheet by remember { mutableStateOf(false) }
    val rssSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newRssInput by remember { mutableStateOf("") }

    var showInterestSheet by remember { mutableStateOf(false) }
    val interestSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newTypeInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(FabAction.openAddRss) {
        if (FabAction.openAddRss) {
            showRssSheet = true
            FabAction.openAddRss = false
        }
    }
    LaunchedEffect(FabAction.openAddInterest) {
        if (FabAction.openAddInterest) {
            showInterestSheet = true
            FabAction.openAddInterest = false
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor   = MaterialTheme.colorScheme.inverseOnSurface,
                    shape          = RoundedCornerShape(12.dp),
                    modifier       = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("DataAgregate", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding      = PaddingValues(top = 16.dp, bottom = 88.dp)
            ) {
                // ── RSS Sources ───────────────────────────────────────────
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = appCardBg()),
                        shape  = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, appCardBorder())
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier          = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.RssFeed,
                                        contentDescription = null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Surse RSS",
                                    color      = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp,
                                    modifier   = Modifier.weight(1f)
                                )
                                val count = uiState.rssSources.count { it.url != uiState.pendingDeleteRssUrl }
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("$count", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                }
                                IconButton(
                                    onClick  = { showRssSheet = true },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Adaugă sursă",
                                        tint     = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            val rssCount = uiState.rssSources.count { it.url != uiState.pendingDeleteRssUrl }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text     = if (rssCount == 0) "Nicio sursă · Apasă + pentru a adăuga"
                                           else "$rssCount ${if (rssCount == 1) "sursă adăugată" else "surse adăugate"}",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // ── Interests ─────────────────────────────────────────────
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = appCardBg()),
                        shape  = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, appCardBorder())
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier         = Modifier
                                        .size(36.dp)
                                        .background(AppScoreGreen.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint     = AppScoreGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Interesele Mele",
                                    color      = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp,
                                    modifier   = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick  = { showInterestSheet = true },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(AppScoreGreen, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Adaugă interes",
                                        tint     = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            val interestCount  = uiState.interests.count { it.name != uiState.pendingDeleteInterest }
                            val selectedCount  = uiState.interests.count { it.isSelected && it.name != uiState.pendingDeleteInterest }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text     = if (interestCount == 0) "Niciun interes · Apasă + pentru a adăuga"
                                           else "$selectedCount active din $interestCount",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // ── Sync ──────────────────────────────────────────────────
                item {
                    Button(
                        onClick = {
                            val category = uiState.interests.firstOrNull { it.isSelected }?.name ?: ""
                            viewModel.saveConfiguration(category, null, 6, 0)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape   = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading &&
                                uiState.rssSources.any { it.isEnabled } &&
                                uiState.interests.any { it.isSelected }
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(22.dp),
                                color       = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronizează", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }

            // ── RSS Bottom Sheet ───────────────────────────────────────────
            if (showRssSheet) {
                ModalBottomSheet(
                    onDismissRequest = { newRssInput = ""; showRssSheet = false },
                    sheetState       = rssSheetState,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    dragHandle = {
                        Box(
                            modifier         = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), CircleShape)
                            )
                        }
                    }
                ) {
                    RssSheetContent(
                        sources       = uiState.rssSources.filter { it.url != uiState.pendingDeleteRssUrl },
                        value         = newRssInput,
                        onValueChange = { newRssInput = it },
                        onAdd = {
                            if (newRssInput.isNotBlank()) {
                                viewModel.addNewRssSource(newRssInput)
                                newRssInput = ""
                            }
                        },
                        onToggle = { viewModel.toggleRssSource(it) },
                        onDelete = { viewModel.scheduleDeleteRssSource(it) }
                    )
                }
            }

            // ── Interest Bottom Sheet ──────────────────────────────────────
            if (showInterestSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        newTypeInput = ""
                        showInterestSheet = false
                    },
                    sheetState    = interestSheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle    = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), CircleShape)
                            )
                        }
                    }
                ) {
                    InterestSheetContent(
                        interests     = uiState.interests.filter { it.name != uiState.pendingDeleteInterest },
                        value         = newTypeInput,
                        onValueChange = { newTypeInput = it },
                        onAdd = {
                            if (newTypeInput.isNotBlank()) {
                                viewModel.addNewDataType(newTypeInput)
                                newTypeInput = ""
                            }
                        },
                        onToggle      = { viewModel.toggleInterest(it) },
                        onDelete      = { viewModel.scheduleDeleteInterest(it.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RssSourceItem(
    source: RssSourceEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor by animateColorAsState(
        targetValue  = if (source.isEnabled) AppScoreGreen else subtleColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label        = "iconColor"
    )
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(iconColor, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                source.url.toDomain(),
                color      = if (source.isEnabled) MaterialTheme.colorScheme.onSurface else subtleColor,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (!source.isHealthy) {
                Text("Feed indisponibil", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
        IconButton(
            onClick  = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.error.copy(alpha = 0.65f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun InterestItem(
    interest: InterestEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textColor by animateColorAsState(
        targetValue  = if (interest.isSelected) MaterialTheme.colorScheme.onSurface else subtleColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label        = "textColor"
    )
    val iconColor by animateColorAsState(
        targetValue  = if (interest.isSelected) AppScoreGreen else subtleColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label        = "iconColor"
    )
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (interest.isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint     = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            interest.name,
            color    = textColor,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick  = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.error.copy(alpha = 0.65f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssSheetContent(
    sources: List<RssSourceEntity>,
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onToggle: (RssSourceEntity) -> Unit,
    onDelete: (RssSourceEntity) -> Unit
) {
    var showAddField by remember { mutableStateOf(false) }
    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val subtle    = MaterialTheme.colorScheme.onSurfaceVariant
    val outline   = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .background(primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.RssFeed, null, tint = primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Surse RSS", color = onSurface, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text(
                    if (sources.isEmpty()) "Nicio sursă adăugată"
                    else "${sources.size} ${if (sources.size == 1) "sursă" else "surse"}",
                    color = subtle, fontSize = 13.sp
                )
            }
        }

        HorizontalDivider(color = onSurface.copy(alpha = 0.08f))

        if (sources.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                sources.forEach { source ->
                    RssSourceItem(
                        source   = source,
                        onToggle = { onToggle(source) },
                        onDelete = { onDelete(source) }
                    )
                }
            }
            HorizontalDivider(color = onSurface.copy(alpha = 0.08f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (showAddField) {
                OutlinedTextField(
                    value           = value,
                    onValueChange   = onValueChange,
                    placeholder     = { Text("https://example.com/feed.xml", color = subtle.copy(alpha = 0.7f), fontSize = 14.sp) },
                    modifier        = Modifier.fillMaxWidth(),
                    textStyle       = LocalTextStyle.current.copy(color = onSurface, fontSize = 15.sp),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (value.isNotBlank()) { onAdd(); showAddField = false }
                    }),
                    leadingIcon = {
                        Icon(Icons.Default.Link, null, tint = subtle, modifier = Modifier.size(18.dp))
                    },
                    shape  = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = onSurface.copy(alpha = 0.06f),
                        focusedContainerColor   = onSurface.copy(alpha = 0.09f),
                        unfocusedBorderColor    = outline,
                        focusedBorderColor      = primary,
                        unfocusedTextColor      = onSurface,
                        focusedTextColor        = onSurface,
                        cursorColor             = primary
                    )
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = { showAddField = false; onValueChange("") },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, outline)
                    ) {
                        Text("Anulează", color = subtle)
                    }
                    Button(
                        onClick  = { onAdd(); showAddField = false },
                        enabled  = value.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = primary,
                            disabledContainerColor = primary.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Adaugă", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick  = { showAddField = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, primary.copy(alpha = 0.5f)),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = primary.copy(alpha = 0.08f),
                        contentColor   = primary
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Adaugă sursă nouă", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestSheetContent(
    interests: List<InterestEntity>,
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onToggle: (InterestEntity) -> Unit,
    onDelete: (InterestEntity) -> Unit
) {
    var showAddField by remember { mutableStateOf(false) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val subtle    = MaterialTheme.colorScheme.onSurfaceVariant
    val outline   = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .background(AppScoreGreen.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, null, tint = AppScoreGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Interesele Mele", color = onSurface, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text(
                    if (interests.isEmpty()) "Niciun interes adăugat"
                    else "${interests.size} ${if (interests.size == 1) "categorie" else "categorii"}",
                    color = subtle, fontSize = 13.sp
                )
            }
        }

        HorizontalDivider(color = onSurface.copy(alpha = 0.08f))

        if (interests.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                interests.forEach { interest ->
                    InterestItem(
                        interest = interest,
                        onToggle = { onToggle(interest) },
                        onDelete = { onDelete(interest) }
                    )
                }
            }
            HorizontalDivider(color = onSurface.copy(alpha = 0.08f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (showAddField) {
                OutlinedTextField(
                    value           = value,
                    onValueChange   = onValueChange,
                    placeholder     = { Text("ex: Quantum Computing, Web3...", color = subtle.copy(alpha = 0.7f), fontSize = 14.sp) },
                    modifier        = Modifier.fillMaxWidth(),
                    textStyle       = LocalTextStyle.current.copy(color = onSurface, fontSize = 15.sp),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (value.isNotBlank()) { onAdd(); showAddField = false }
                    }),
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = subtle, modifier = Modifier.size(18.dp))
                    },
                    shape  = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = onSurface.copy(alpha = 0.06f),
                        focusedContainerColor   = onSurface.copy(alpha = 0.09f),
                        unfocusedBorderColor    = outline,
                        focusedBorderColor      = AppScoreGreen,
                        unfocusedTextColor      = onSurface,
                        focusedTextColor        = onSurface,
                        cursorColor             = AppScoreGreen
                    )
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = { showAddField = false; onValueChange("") },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, outline)
                    ) {
                        Text("Anulează", color = subtle)
                    }
                    Button(
                        onClick  = { onAdd(); showAddField = false },
                        enabled  = value.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = AppScoreGreen,
                            disabledContainerColor = AppScoreGreen.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Adaugă", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick  = { showAddField = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, AppScoreGreen.copy(alpha = 0.5f)),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = AppScoreGreen.copy(alpha = 0.08f),
                        contentColor   = AppScoreGreen
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Adaugă interes nou", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}
