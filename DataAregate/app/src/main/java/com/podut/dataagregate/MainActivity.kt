package com.podut.dataagregate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.podut.dataagregate.feature.categories.navigation.categoriesScreen
import com.podut.dataagregate.feature.category_articles.navigation.categoryArticlesScreen
import com.podut.dataagregate.feature.home.navigation.homeScreen
import com.podut.dataagregate.feature.explore.navigation.exploreScreen
import com.podut.dataagregate.feature.profile.navigation.profileScreen
import com.podut.dataagregate.feature.article_reader.navigation.articleReaderScreen
import com.podut.dataagregate.feature.news_feed.navigation.newsFeedScreen
import com.podut.dataagregate.feature.saved.navigation.savedScreen
import com.podut.dataagregate.feature.home.navigation.HOME_ROUTE
import com.podut.dataagregate.feature.news_feed.navigation.NEWS_FEED_ROUTE
import com.podut.dataagregate.feature.explore.navigation.EXPLORE_ROUTE
import com.podut.dataagregate.feature.saved.navigation.SAVED_ROUTE
import com.podut.dataagregate.feature.profile.navigation.PROFILE_ROUTE
import com.podut.dataagregate.core.ui.DataAgregateTheme
import com.podut.dataagregate.theme.ThemePreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreference: ThemePreference

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by themePreference.isDarkTheme.collectAsState(initial = true)
            val toggleTheme: () -> Unit = {
                lifecycleScope.launch { themePreference.setDarkTheme(!isDarkTheme) }
            }
            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact

            DataAgregateTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: NEWS_FEED_ROUTE

                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF8A2BE2),
                    selectedTextColor = Color(0xFF8A2BE2),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )

                val navigateToArticle: (String, String) -> Unit = { url, title ->
                    navController.navigate("article_reader?url=${Uri.encode(url)}&title=${Uri.encode(title)}")
                }

                Scaffold(
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar(
                                containerColor = if (isDarkTheme) Color(0xFF0F0F17) else Color.White,
                                contentColor = if (isDarkTheme) Color.White else Color.Black
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, null) },
                                    label = { Text("Home", fontSize = 10.sp) },
                                    selected = currentRoute == NEWS_FEED_ROUTE,
                                    onClick = { navController.navigate(NEWS_FEED_ROUTE) },
                                    colors = navItemColors
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Explore, null) },
                                    label = { Text("Explore", fontSize = 10.sp) },
                                    selected = currentRoute == EXPLORE_ROUTE,
                                    onClick = { navController.navigate(EXPLORE_ROUTE) },
                                    colors = navItemColors
                                )
                                NavigationBarItem(
                                    icon = {
                                        Surface(
                                            modifier = Modifier.size(48.dp),
                                            shape = CircleShape,
                                            color = Color(0xFF8A2BE2)
                                        ) {
                                            Icon(
                                                Icons.Default.Add, null,
                                                modifier = Modifier.padding(8.dp),
                                                tint = Color.White
                                            )
                                        }
                                    },
                                    label = {},
                                    selected = false,
                                    onClick = { navController.navigate(HOME_ROUTE) }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Bookmark, null) },
                                    label = { Text("Saved", fontSize = 10.sp) },
                                    selected = currentRoute == SAVED_ROUTE,
                                    onClick = { navController.navigate(SAVED_ROUTE) },
                                    colors = navItemColors
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, null) },
                                    label = { Text("Settings", fontSize = 10.sp) },
                                    selected = currentRoute == PROFILE_ROUTE,
                                    onClick = { navController.navigate(PROFILE_ROUTE) },
                                    colors = navItemColors
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (useNavRail) {
                            NavigationRail(
                                containerColor = if (isDarkTheme) Color(0xFF0F0F17) else Color.White
                            ) {
                                NavigationRailItem(
                                    icon = { Icon(Icons.Default.Home, null) },
                                    label = { Text("Home") },
                                    selected = currentRoute == NEWS_FEED_ROUTE,
                                    onClick = { navController.navigate(NEWS_FEED_ROUTE) }
                                )
                                NavigationRailItem(
                                    icon = { Icon(Icons.Default.Explore, null) },
                                    label = { Text("Explore") },
                                    selected = currentRoute == EXPLORE_ROUTE,
                                    onClick = { navController.navigate(EXPLORE_ROUTE) }
                                )
                                NavigationRailItem(
                                    icon = { Icon(Icons.Default.Bookmark, null) },
                                    label = { Text("Saved") },
                                    selected = currentRoute == SAVED_ROUTE,
                                    onClick = { navController.navigate(SAVED_ROUTE) }
                                )
                                NavigationRailItem(
                                    icon = { Icon(Icons.Default.Settings, null) },
                                    label = { Text("Settings") },
                                    selected = currentRoute == PROFILE_ROUTE,
                                    onClick = { navController.navigate(PROFILE_ROUTE) }
                                )
                            }
                        }
                        
                        NavHost(
                            navController = navController,
                            startDestination = NEWS_FEED_ROUTE,
                            modifier = Modifier.weight(1f)
                        ) {
                            homeScreen()
                            newsFeedScreen(onArticleClick = navigateToArticle)
                            exploreScreen(
                                onCategoryClick = { cat ->
                                    navController.navigate("category_articles/${Uri.encode(cat)}")
                                }
                            )
                            categoryArticlesScreen(
                                onArticleClick = navigateToArticle,
                                onBack = { navController.popBackStack() }
                            )
                            profileScreen(
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = toggleTheme
                            )
                            categoriesScreen()
                            savedScreen(onArticleClick = navigateToArticle)
                            articleReaderScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
