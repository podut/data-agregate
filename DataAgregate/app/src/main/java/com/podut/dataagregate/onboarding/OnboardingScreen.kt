package com.podut.dataagregate.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val Purple     = Color(0xFF8A2BE2)
private val DarkBg     = Color(0xFF0F0F17)
private val CardBg     = Color(0xFF1A1A2E)
private val SelectedBg = Color(0xFF3D1A6E)
private val SubtleText = Color(0xFF8888A0)
private val DimBorder  = Color(0xFF2A2A3E)

private val INTERESTS = listOf(
    "AI", "Dev", "Tech", "Business", "Security",
    "Science", "Health", "Crypto", "Startups", "Gaming",
    "Romania", "Politics", "Climate", "Space", "Finance", "Design"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onDone: () -> Unit
) {
    val selectedInterests = viewModel.selectedInterests
    val selectedLanguage  = viewModel.selectedLanguage
    val isSyncing         = viewModel.isSyncing
    var step by remember { mutableIntStateOf(1) }

    BackHandler(enabled = step == 2) { step = 1 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(top = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Purple, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "DataAgregate",
                    color      = Color.White,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("Your personalized news feed", color = SubtleText, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f).height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Purple)
                )
                Box(
                    modifier = Modifier
                        .weight(1f).height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (step == 2) Purple else DimBorder)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("Step $step of 2", color = SubtleText, fontSize = 12.sp)
        }

        // ── Content ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 24.dp)
        ) {
            if (step == 1) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "What are you\ninterested in?",
                        color      = Color.White,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Select at least 3 topics — we'll set up your feed automatically",
                        color    = SubtleText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        INTERESTS.forEach { interest ->
                            val selected = interest in selectedInterests
                            FilterChip(
                                selected = selected,
                                onClick  = { viewModel.toggleInterest(interest) },
                                label    = { Text(interest, fontSize = 13.sp) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor  = SelectedBg,
                                    selectedLabelColor      = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor          = CardBg,
                                    labelColor              = SubtleText
                                )
                            )
                        }
                    }
                    if (selectedInterests.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("${selectedInterests.size} selected", color = Purple, fontSize = 13.sp)
                    }
                }
            } else {
                Column {
                    Text(
                        "Choose your\nlanguage",
                        color      = Color.White,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(32.dp))
                    LanguageCard(
                        flag     = "🇷🇴",
                        label    = "Română",
                        selected = selectedLanguage == "ro",
                        onClick  = { viewModel.selectLanguage("ro") }
                    )
                    Spacer(Modifier.height(12.dp))
                    LanguageCard(
                        flag     = "🇬🇧",
                        label    = "English",
                        selected = selectedLanguage == "en",
                        onClick  = { viewModel.selectLanguage("en") }
                    )
                }
            }
        }

        // ── CTA Button ────────────────────────────────────────────────────────
        Button(
            onClick = {
                if (step == 1) step = 2
                else viewModel.completeOnboarding(onDone)
            },
            enabled = (step == 1 && selectedInterests.size >= 3 || step == 2) && !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = Purple,
                disabledContainerColor = Color(0xFF3A1A5E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    color       = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text       = if (step == 1) "Continue  →" else "Get Started  →",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageCard(
    flag: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (selected) SelectedBg else CardBg
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Purple else DimBorder
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier          = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(flag, fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            if (selected) {
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint     = Purple,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
