package com.podut.dataagregate.feature.categories.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.podut.dataagregate.feature.categories.CategoriesScreen

const val CATEGORIES_ROUTE = "categories"

fun NavGraphBuilder.categoriesScreen() {
    composable(CATEGORIES_ROUTE) {
        CategoriesScreen()
    }
}
