package com.podut.dataagregate.feature.article_reader.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.podut.dataagregate.feature.article_reader.ArticleReaderScreen

const val ARTICLE_READER_ROUTE = "article_reader?url={url}&title={title}"

fun NavGraphBuilder.articleReaderScreen(onBack: () -> Unit) {
    composable(
        route = ARTICLE_READER_ROUTE,
        arguments = listOf(
            navArgument("url")   { type = NavType.StringType },
            navArgument("title") { type = NavType.StringType }
        )
    ) { back ->
        val url = back.arguments?.getString("url") ?: ""
        val title = back.arguments?.getString("title") ?: ""
        ArticleReaderScreen(
            url = url,
            title = title,
            onBack = onBack
        )
    }
}
