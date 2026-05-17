package com.podut.dataagregate.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.podut.dataagregate.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val bg = if (isDarkTheme) Color(0xFF0F0F17) else Color(0xFFF5F5F7)
    val surface = if (isDarkTheme) Color(0xFF1E1E2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val onBg = if (isDarkTheme) Color.White else Color.Black
    val textSec = Color.Gray
    val purple = Color(0xFF8A2BE2)

    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Image(
            painter = painterResource(id = R.drawable.bg_settings),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // Avatar + name
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(purple)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    uiState.name,
                    color      = onBg,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { 
                    editName = uiState.name
                    showEditDialog = true 
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = purple, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                "Powered by DataAgregate",
                color    = textSec,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = onBg.copy(alpha = 0.1f))
            Spacer(Modifier.height(24.dp))

            // Appearance section
            Text("Settings", color = onBg, style = MaterialTheme.typography.labelLarge, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(12.dp))

            Surface(
                shape  = RoundedCornerShape(16.dp),
                color  = surface,
                tonalElevation = 0.dp
            ) {
                Column {
                    // Theme Toggle
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector        = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint               = purple,
                            modifier           = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isDarkTheme) "Dark Mode" else "Light Mode",
                                color      = onBg,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
                                color    = textSec,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked         = isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors          = SwitchDefaults.colors(
                                checkedThumbColor   = Color.White,
                                checkedTrackColor   = purple,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray
                            )
                        )
                    }
                    
                    HorizontalDivider(color = onBg.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Language Selector
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = purple,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("News Language", color = onBg, style = MaterialTheme.typography.titleMedium)
                            Text("AI translated articles", color = textSec, style = MaterialTheme.typography.bodySmall)
                        }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(
                                    text = if (uiState.language == "ro") "Romanian" else "English",
                                    color = purple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                containerColor = surface
                            ) {
                                DropdownMenuItem(
                                    text = { Text("English", color = onBg) },
                                    onClick = {
                                        viewModel.updateProfile(uiState.name, "en")
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Romanian", color = onBg) },
                                    onClick = {
                                        viewModel.updateProfile(uiState.name, "ro")
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile", color = onBg) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = purple,
                        focusedLabelColor = purple,
                        focusedTextColor = onBg,
                        unfocusedTextColor = onBg
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.updateProfile(editName, uiState.language)
                    showEditDialog = false 
                }) {
                    Text("SAVE", color = purple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("CANCEL", color = textSec)
                }
            },
            containerColor = surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
