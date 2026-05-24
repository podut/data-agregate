package com.podut.dataagregate

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.podut.dataagregate.core.ui.DataAgregateTheme
import com.podut.dataagregate.core.ui.appNavBg
import com.podut.dataagregate.feature.article_reader.navigation.articleReaderScreen
import com.podut.dataagregate.feature.categories.navigation.categoriesScreen
import com.podut.dataagregate.feature.category_articles.navigation.categoryArticlesScreen
import com.podut.dataagregate.feature.explore.navigation.EXPLORE_ROUTE
import com.podut.dataagregate.feature.explore.navigation.exploreScreen
import com.podut.dataagregate.feature.home.HomeViewModel
import com.podut.dataagregate.feature.home.InterestSheetContent
import com.podut.dataagregate.feature.home.RssSheetContent
import com.podut.dataagregate.feature.news_feed.navigation.NEWS_FEED_ROUTE
import com.podut.dataagregate.feature.news_feed.navigation.newsFeedScreen
import com.podut.dataagregate.feature.profile.navigation.PROFILE_ROUTE
import com.podut.dataagregate.feature.profile.navigation.profileScreen
import com.podut.dataagregate.feature.saved.navigation.SAVED_ROUTE
import com.podut.dataagregate.feature.saved.navigation.savedScreen
import com.podut.dataagregate.onboarding.ONBOARDING_ROUTE
import com.podut.dataagregate.onboarding.onboardingScreen
import com.podut.dataagregate.core.ui.preferences.OnboardingPreference
import com.podut.dataagregate.core.ui.preferences.ThemePreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private val SheetBg = Color(0xFF13132A)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreference: ThemePreference
    @Inject lateinit var onboardingPreference: OnboardingPreference

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme      by themePreference.isDarkTheme.collectAsState(initial = true)
            val onboardingDone   by onboardingPreference.onboardingCompleted.collectAsState(initial = null)
            val windowSizeClass  = calculateWindowSizeClass(this)
            val useNavRail       = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact

            val toggleTheme: () -> Unit = {
                lifecycleScope.launch { themePreference.setDarkTheme(!isDarkTheme) }
            }

            DataAgregateTheme(darkTheme = isDarkTheme) {
                if (onboardingDone == null) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F17)))
                    return@DataAgregateTheme
                }

                val startDestination = if (onboardingDone == true) NEWS_FEED_ROUTE else ONBOARDING_ROUTE

                val navController     = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute      = navBackStackEntry?.destination?.route ?: startDestination
                val isOnboarding      = currentRoute == ONBOARDING_ROUTE

                // ── HomeViewModel — activity-scoped for sheets ─────────────
                val homeViewModel: HomeViewModel = hiltViewModel()
                val homeState by homeViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) { homeViewModel.init() }

                // ── Sheet state ────────────────────────────────────────────
                var showRssSheet      by remember { mutableStateOf(false) }
                val rssSheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var newRssInput       by remember { mutableStateOf("") }

                var showInterestSheet by remember { mutableStateOf(false) }
                val interestSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var newTypeInput      by remember { mutableStateOf("") }

                val navigateToArticle: (String, String) -> Unit = { url, title ->
                    navController.navigate("article_reader?url=${Uri.encode(url)}&title=${Uri.encode(title)}")
                }

                var fabExpanded by remember { mutableStateOf(false) }
                val fabRotation by animateFloatAsState(
                    targetValue   = if (fabExpanded) 45f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label         = "fabRot"
                )

                val appBg = MaterialTheme.colorScheme.background

                Scaffold(
                    containerColor = appBg,
                    bottomBar = {
                        if (!useNavRail && !isOnboarding) {
                            val navBotPad    = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            val barBg        = appNavBg()
                            val activeTint   = MaterialTheme.colorScheme.primary
                            val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color    = barBg,
                                    shadowElevation = 8.dp
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier               = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp),
                                            horizontalArrangement  = Arrangement.SpaceEvenly,
                                            verticalAlignment      = Alignment.CenterVertically
                                        ) {
                                            // Acasă
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { navController.navigate(NEWS_FEED_ROUTE) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Home,
                                                    contentDescription = "Acasă",
                                                    tint = if (currentRoute == NEWS_FEED_ROUTE) activeTint else inactiveTint,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Acasă",
                                                    color = if (currentRoute == NEWS_FEED_ROUTE) activeTint else inactiveTint,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentRoute == NEWS_FEED_ROUTE) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }

                                            // Explorează
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { navController.navigate(EXPLORE_ROUTE) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Explore,
                                                    contentDescription = "Explorează",
                                                    tint = if (currentRoute == EXPLORE_ROUTE) activeTint else inactiveTint,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Explorează",
                                                    color = if (currentRoute == EXPLORE_ROUTE) activeTint else inactiveTint,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentRoute == EXPLORE_ROUTE) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }

                                            // Spacer pentru a lăsa loc FAB-ului plutitor
                                            Spacer(modifier = Modifier.weight(1f))

                                            // Salvate
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { navController.navigate(SAVED_ROUTE) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bookmark,
                                                    contentDescription = "Salvate",
                                                    tint = if (currentRoute == SAVED_ROUTE) activeTint else inactiveTint,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Salvate",
                                                    color = if (currentRoute == SAVED_ROUTE) activeTint else inactiveTint,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentRoute == SAVED_ROUTE) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }

                                            // Setări
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { navController.navigate(PROFILE_ROUTE) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = "Setări",
                                                    tint = if (currentRoute == PROFILE_ROUTE) activeTint else inactiveTint,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Setări",
                                                    color = if (currentRoute == PROFILE_ROUTE) activeTint else inactiveTint,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentRoute == PROFILE_ROUTE) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                        if (navBotPad > 0.dp) {
                                            Spacer(Modifier.fillMaxWidth().height(navBotPad))
                                        }
                                    }
                                }

                                // FAB central plutitor (docked)
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = navBotPad + 20.dp)
                                        .size(58.dp)
                                        .shadow(elevation = 12.dp, shape = CircleShape)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF8A2BE2), Color(0xFFD81B60))
                                            ),
                                            shape = CircleShape
                                        )
                                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .clip(CircleShape)
                                        .clickable { fabExpanded = !fabExpanded },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Adaugă",
                                        modifier = Modifier.size(28.dp).rotate(fabRotation),
                                        tint     = Color.White
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().background(appBg)) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            if (useNavRail && !isOnboarding) {
                                NavigationRail(
                                    containerColor = appNavBg(),
                                    windowInsets   = WindowInsets.safeDrawing.only(
                                        WindowInsetsSides.Start + WindowInsetsSides.Vertical
                                    )
                                ) {
                                    Spacer(Modifier.height(8.dp))
                                    NavigationRailItem(
                                        icon     = { Icon(Icons.Default.Home, null) },
                                        label    = { Text("Home") },
                                        selected = currentRoute == NEWS_FEED_ROUTE,
                                        onClick  = { navController.navigate(NEWS_FEED_ROUTE) }
                                    )
                                    NavigationRailItem(
                                        icon     = { Icon(Icons.Default.Explore, null) },
                                        label    = { Text("Explore") },
                                        selected = currentRoute == EXPLORE_ROUTE,
                                        onClick  = { navController.navigate(EXPLORE_ROUTE) }
                                    )
                                    NavigationRailItem(
                                        icon     = { Icon(Icons.Default.Bookmark, null) },
                                        label    = { Text("Saved") },
                                        selected = currentRoute == SAVED_ROUTE,
                                        onClick  = { navController.navigate(SAVED_ROUTE) }
                                    )
                                    NavigationRailItem(
                                        icon     = { Icon(Icons.Default.Settings, null) },
                                        label    = { Text("Settings") },
                                        selected = currentRoute == PROFILE_ROUTE,
                                        onClick  = { navController.navigate(PROFILE_ROUTE) }
                                    )
                                }
                            }

                            NavHost(
                                navController    = navController,
                                startDestination = startDestination,
                                modifier         = Modifier
                                    .weight(1f)
                                    .then(
                                        if (useNavRail) Modifier.windowInsetsPadding(
                                            WindowInsets.safeDrawing.only(WindowInsetsSides.End)
                                        ) else Modifier
                                    )
                            ) {
                                onboardingScreen(onDone = {
                                    navController.navigate(NEWS_FEED_ROUTE) {
                                        popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                                    }
                                })
                                newsFeedScreen(onArticleClick = navigateToArticle)
                                exploreScreen(
                                    onCategoryClick = { cat ->
                                        navController.navigate("category_articles/${Uri.encode(cat)}")
                                    }
                                )
                                categoryArticlesScreen(
                                    onArticleClick = navigateToArticle,
                                    onBack         = { navController.popBackStack() }
                                )
                                profileScreen(
                                    onToggleTheme     = toggleTheme,
                                    onResetOnboarding = {
                                        navController.navigate(ONBOARDING_ROUTE) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                                categoriesScreen()
                                savedScreen(onArticleClick = navigateToArticle)
                                articleReaderScreen(onBack = { navController.popBackStack() })
                            }
                        }

                        // ── Speed dial overlay ─────────────────────────────
                        if (!useNavRail && !isOnboarding) {
                            AnimatedVisibility(
                                visible = fabExpanded,
                                enter   = fadeIn(),
                                exit    = fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable(
                                            indication        = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { fabExpanded = false }
                                )
                            }
                            AnimatedVisibility(
                                visible = fabExpanded,
                                enter   = slideInVertically { it } + fadeIn(),
                                exit    = slideOutVertically { it } + fadeOut()
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = innerPadding.calculateBottomPadding() + 12.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        SpeedDialRow(
                                            icon  = Icons.Default.Star,
                                            label = "Adaugă Interes",
                                            color = Color(0xFF22C55E)
                                        ) {
                                            fabExpanded = false
                                            showInterestSheet = true
                                        }
                                        SpeedDialRow(
                                            icon  = Icons.Default.RssFeed,
                                            label = "Adaugă Sursă RSS",
                                            color = Color(0xFF8A2BE2)
                                        ) {
                                            fabExpanded = false
                                            showRssSheet = true
                                        }
                                    }
                                }
                            }
                        }

                        // ── RSS Bottom Sheet ───────────────────────────────
                        if (showRssSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { newRssInput = ""; showRssSheet = false },
                                sheetState       = rssSheetState,
                                containerColor   = SheetBg,
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
                                                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                        )
                                    }
                                }
                            ) {
                                RssSheetContent(
                                    sources       = homeState.rssSources.filter { it.url != homeState.pendingDeleteRssUrl },
                                    value         = newRssInput,
                                    onValueChange = { newRssInput = it },
                                    onAdd = {
                                        if (newRssInput.isNotBlank()) {
                                            homeViewModel.addNewRssSource(newRssInput)
                                            newRssInput = ""
                                        }
                                    },
                                    onToggle = { homeViewModel.toggleRssSource(it) },
                                    onDelete = { homeViewModel.scheduleDeleteRssSource(it) }
                                )
                            }
                        }

                        // ── Interest Bottom Sheet ──────────────────────────
                        if (showInterestSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { newTypeInput = ""; showInterestSheet = false },
                                sheetState       = interestSheetState,
                                containerColor   = SheetBg,
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
                                                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                        )
                                    }
                                }
                            ) {
                                InterestSheetContent(
                                    interests     = homeState.interests.filter { it.name != homeState.pendingDeleteInterest },
                                    value         = newTypeInput,
                                    onValueChange = { newTypeInput = it },
                                    onAdd = {
                                        if (newTypeInput.isNotBlank()) {
                                            homeViewModel.addNewDataType(newTypeInput)
                                            newTypeInput = ""
                                        }
                                    },
                                    onToggle = { homeViewModel.toggleInterest(it) },
                                    onDelete = { homeViewModel.scheduleDeleteInterest(it.name) }
                                )
                            }
                        }
                    } // end Box
                }
            }
        }
    }
}

@Composable
private fun SpeedDialRow(
    icon    : ImageVector,
    label   : String,
    color   : Color,
    onClick : () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            indication        = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Text(
            label,
            color    = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp)
        )
        Spacer(Modifier.width(10.dp))
        Surface(
            shape    = CircleShape,
            color    = color,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
