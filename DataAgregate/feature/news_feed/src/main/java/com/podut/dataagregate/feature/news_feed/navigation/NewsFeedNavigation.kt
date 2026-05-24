package com.podut.dataagregate.feature.news_feed.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.podut.dataagregate.feature.news_feed.NewsFeedScreen

const val NEWS_FEED_ROUTE = "news_feed"

fun NavGraphBuilder.newsFeedScreen(onArticleClick: (String, String) -> Unit) {
    composable(NEWS_FEED_ROUTE) {
        NewsFeedScreen(onArticleClick = onArticleClick)
    }
}
