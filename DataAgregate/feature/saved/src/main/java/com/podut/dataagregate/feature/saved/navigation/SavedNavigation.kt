package com.podut.dataagregate.feature.saved.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.podut.dataagregate.feature.saved.SavedScreen

const val SAVED_ROUTE = "saved"

fun NavGraphBuilder.savedScreen(onArticleClick: (String, String) -> Unit) {
    composable(SAVED_ROUTE) {
        SavedScreen(onArticleClick = onArticleClick)
    }
}
