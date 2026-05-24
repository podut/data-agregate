package com.podut.dataagregate.feature.explore.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.podut.dataagregate.feature.explore.ExploreScreen

const val EXPLORE_ROUTE = "explore"

fun NavGraphBuilder.exploreScreen(onCategoryClick: (String) -> Unit) {
    composable(EXPLORE_ROUTE) {
        ExploreScreen(onCategoryClick = onCategoryClick)
    }
}
