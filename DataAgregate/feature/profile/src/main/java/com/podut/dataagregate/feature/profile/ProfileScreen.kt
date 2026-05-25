package com.podut.dataagregate.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.podut.dataagregate.core.ui.AppError
import com.podut.dataagregate.core.ui.R
import com.podut.dataagregate.core.ui.appBg
import com.podut.dataagregate.core.ui.appBgImageAlpha
import com.podut.dataagregate.core.ui.appCardBg
import com.podut.dataagregate.core.ui.appChipBg
import com.podut.dataagregate.core.ui.appChipBorder
import com.podut.dataagregate.core.ui.appDivider
import com.podut.dataagregate.core.ui.appOnBg
import com.podut.dataagregate.core.ui.appTextSec
import com.podut.dataagregate.core.ui.LocalIsDarkTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onToggleTheme: () -> Unit,
    onResetOnboarding: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState  by viewModel.uiState.collectAsState()
    val isDark   = LocalIsDarkTheme.current
    val context  = LocalContext.current

    val bg       = appBg()
    val cardBg   = appCardBg()
    val onBg     = appOnBg()
    val textSec  = appTextSec()
    val divider  = appDivider()
    val primary  = MaterialTheme.colorScheme.primary
    val bgAlpha  = appBgImageAlpha()

    var showEditDialog  by remember { mutableStateOf(false) }
    var editName        by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Image(
            painter            = painterResource(id = R.drawable.bg_settings),
            contentDescription = null,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop,
            alpha              = bgAlpha
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Avatar ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(primary)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(uiState.name, color = onBg, style = MaterialTheme.typography.titleLarge)
                IconButton(
                    onClick  = { editName = uiState.name; showEditDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = primary, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                "Powered by DataAgregate",
                color    = textSec,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = divider)
            Spacer(Modifier.height(24.dp))

            // ── Settings section ──────────────────────────────────────────────
            Text("Settings", color = textSec, style = MaterialTheme.typography.labelLarge, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(12.dp))

            Surface(
                shape          = RoundedCornerShape(20.dp),
                color          = cardBg,
                tonalElevation = 0.dp,
                border         = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column {
                    SettingsRow(
                        icon   = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        title  = if (isDark) "Dark Mode" else "Light Mode",
                        sub    = if (isDark) "Switch to light theme" else "Switch to dark theme",
                        primary = primary, onBg = onBg, textSec = textSec
                    ) {
                        Switch(
                            checked         = isDark,
                            onCheckedChange = { onToggleTheme() },
                            colors          = SwitchDefaults.colors(
                                checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor   = primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(color = divider, modifier = Modifier.padding(horizontal = 16.dp))

                    var expanded by remember { mutableStateOf(false) }
                    SettingsRow(
                        icon    = Icons.Default.Language,
                        title   = "News Language",
                        sub     = "AI translated articles",
                        primary = primary, onBg = onBg, textSec = textSec
                    ) {
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(
                                    text       = if (uiState.language == "ro") "Română" else "English",
                                    color      = primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            DropdownMenu(
                                expanded         = expanded,
                                onDismissRequest = { expanded = false },
                                containerColor   = MaterialTheme.colorScheme.surface
                            ) {
                                DropdownMenuItem(
                                    text    = { Text("English", color = onBg) },
                                    onClick = { viewModel.updateProfile(uiState.name, "en"); expanded = false }
                                )
                                DropdownMenuItem(
                                    text    = { Text("Română", color = onBg) },
                                    onClick = { viewModel.updateProfile(uiState.name, "ro"); expanded = false }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Interests section ─────────────────────────────────────────────
            Text("Your Interests", color = textSec, style = MaterialTheme.typography.labelLarge, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(12.dp))

            Surface(
                shape          = RoundedCornerShape(20.dp),
                color          = cardBg,
                tonalElevation = 0.dp,
                border         = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.favoriteCategories.isEmpty()) {
                        Text(
                            "No interests set yet. Tap Reset below to choose.",
                            color = textSec, fontSize = 14.sp
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.favoriteCategories.forEach { cat ->
                                Surface(
                                    shape  = RoundedCornerShape(20.dp),
                                    color  = appChipBg(),
                                    border = BorderStroke(1.dp, appChipBorder())
                                ) {
                                    Text(
                                        cat,
                                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color      = primary,
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = divider)
                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Reset Interests", color = onBg, style = MaterialTheme.typography.titleMedium)
                            Text("Choose new topics from scratch", color = textSec, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { showResetDialog = true }) {
                            Text("Reset", color = AppError, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── About & Contact section ───────────────────────────────────────
            Text("About & Contact", color = textSec, style = MaterialTheme.typography.labelLarge, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(12.dp))

            Surface(
                shape          = RoundedCornerShape(20.dp),
                color          = cardBg,
                tonalElevation = 0.dp,
                border         = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column {
                    // Email contact
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Email, null, tint = primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Contact & Support", color = onBg, style = MaterialTheme.typography.titleMedium)
                            Text("podutpetru@gmail.com", color = textSec, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:podutpetru@gmail.com"))
                            intent.putExtra(Intent.EXTRA_SUBJECT, "DataAgregate Support")
                            context.startActivity(Intent.createChooser(intent, "Send email"))
                        }) {
                            Text("Email", color = primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = divider, modifier = Modifier.padding(horizontal = 16.dp))

                    // Despre aplicație
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Despre DataAgregate", color = onBg, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Agregator de știri tech cu sumarizare AI. Articolele sunt preluate din surse RSS publice și procesate cu Google Gemini 2.5.",
                                color = textSec, style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    HorizontalDivider(color = divider, modifier = Modifier.padding(horizontal = 16.dp))

                    // Versiune
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NewReleases, null, tint = primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Versiune", color = onBg, style = MaterialTheme.typography.titleMedium)
                            Text("1.0.0 · Surse RSS actualizate automat", color = textSec, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Edit name dialog ──────────────────────────────────────────────────────
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title            = { Text("Edit Profile", color = onBg) },
            text             = {
                OutlinedTextField(
                    value         = editName,
                    onValueChange = { editName = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = primary,
                        focusedLabelColor    = primary,
                        focusedTextColor     = onBg,
                        unfocusedTextColor   = onBg
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.updateProfile(editName, uiState.language); showEditDialog = false }) {
                    Text("SAVE", color = primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("CANCEL", color = textSec)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape          = RoundedCornerShape(24.dp)
        )
    }

    // ── Reset interests dialog ────────────────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title            = { Text("Reset Interests?", color = onBg) },
            text             = {
                Text(
                    "You'll go back to the onboarding screen to choose new topics. " +
                    "Your current selections will be replaced.",
                    color = textSec
                )
            },
            confirmButton = {
                TextButton(onClick = { showResetDialog = false; viewModel.resetOnboarding(onDone = onResetOnboarding) }) {
                    Text("RESET", color = AppError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("CANCEL", color = textSec)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape          = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    sub: String,
    primary: androidx.compose.ui.graphics.Color,
    onBg: androidx.compose.ui.graphics.Color,
    textSec: androidx.compose.ui.graphics.Color,
    action: @Composable () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = onBg, style = MaterialTheme.typography.titleMedium)
            Text(sub, color = textSec, style = MaterialTheme.typography.bodySmall)
        }
        action()
    }
}
