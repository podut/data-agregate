package com.podut.dataagregate.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.podut.dataagregate.core.database.entity.RssSourceEntity
import com.podut.dataagregate.core.database.entity.InterestEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.init()
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    
    val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
    var showTimePicker by remember { mutableStateOf(false) }

    var newRssInput by remember { mutableStateOf("") }
    var newTypeInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configurare Agregator", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF673AB7),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F17))) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Secțiune RSS cu listă limitată (max 3 elemente vizibile)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Surse RSS", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            
                            Box(modifier = Modifier.heightIn(max = 200.dp)) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    // Afişăm toate sursele cu excepţia celei în aşteptare de ştergere
                                    uiState.rssSources
                                        .filter { it.url != uiState.pendingDeleteRssUrl }
                                        .forEach { source ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.toggleRssSource(source) }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (source.isEnabled) Color(0xFF4CAF50) else Color.Gray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    source.url,
                                                    color = if (source.isEnabled) Color.White else Color.Gray,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.scheduleDeleteRssSource(source) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = null,
                                                        tint = Color(0xFFEF5350),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    // Banner undo pentru RSS
                                    if (uiState.pendingDeleteRssUrl != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF4E1414), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Sursă ştearsă...",
                                                color = Color(0xFFEF9A9A),
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(onClick = { viewModel.undoDeleteRssSource() }) {
                                                Text("ANULEAZĂ", color = Color(0xFFEF5350), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = newRssInput,
                                onValueChange = { newRssInput = it },
                                placeholder = { Text("URL RSS nou...", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        if (newRssInput.isNotBlank()) {
                                            viewModel.addNewRssSource(newRssInput)
                                            newRssInput = ""
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Adaugă", tint = Color.White)
                                    }
                                }
                            )
                        }
                    }
                }

                // Secțiune Interese Dinamice
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Interesele Mele", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            
                            // Afişăm toate interesele cu excepţia celui în aşteptare de ştergere
                            uiState.interests
                                .filter { it.name != uiState.pendingDeleteInterest }
                                .forEach { interest ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleInterest(interest) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (interest.isSelected) Color(0xFF4CAF50) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            interest.name,
                                            color = if (interest.isSelected) Color.White else Color.Gray,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.scheduleDeleteInterest(interest.name) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color(0xFFEF5350),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            // Banner undo pentru interes
                            if (uiState.pendingDeleteInterest != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF4E1414), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "\"${uiState.pendingDeleteInterest}\" şters...",
                                        color = Color(0xFFEF9A9A),
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.undoDeleteInterest() }) {
                                        Text("ANULEAZĂ", color = Color(0xFFEF5350), fontSize = 12.sp)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newTypeInput,
                                onValueChange = { newTypeInput = it },
                                placeholder = { Text("Adaugă interes nou...", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        if (newTypeInput.isNotBlank()) {
                                            viewModel.addNewDataType(newTypeInput)
                                            newTypeInput = ""
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Adaugă", tint = Color.White)
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { 
                            val category = uiState.interests.firstOrNull { it.isSelected }?.name ?: ""
                            viewModel.saveConfiguration(
                                category, 
                                datePickerState.selectedDateMillis,
                                timePickerState.hour,
                                timePickerState.minute
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                        shape = MaterialTheme.shapes.medium,
                        enabled = !uiState.isLoading && uiState.rssSources.any { it.isEnabled } && uiState.interests.any { it.isSelected }
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Sincronizează Datele", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                }
            }

            // Dialogs
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("OK") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("OK") }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }
        }
    }
}
