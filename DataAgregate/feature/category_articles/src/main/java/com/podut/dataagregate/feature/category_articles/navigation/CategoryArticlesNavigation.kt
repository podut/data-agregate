package com.podut.dataagregate.feature.category_articles.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.podut.dataagregate.feature.category_articles.CategoryArticlesScreen
import android.net.Uri

const val CATEGORY_ARTICLES_BASE_ROUTE = "category_articles"
const val CATEGORY_ARTICLES_ROUTE = "$CATEGORY_ARTICLES_BASE_ROUTE/{category}"

fun NavGraphBuilder.categoryArticlesScreen(
    onArticleClick: (String, String) -> Unit,
    onBack: () -> Unit
) {
    composable(
        route = CATEGORY_ARTICLES_ROUTE,
        arguments = listOf(navArgument("category") { type = NavType.StringType })
    ) { back ->
        val category = back.arguments?.getString("category") ?: ""
        CategoryArticlesScreen(
            category = category,
            onArticleClick = onArticleClick,
            onBack = onBack
        )
    }
}
