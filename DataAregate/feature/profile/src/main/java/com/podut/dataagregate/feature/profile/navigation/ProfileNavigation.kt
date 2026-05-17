package com.podut.dataagregate.feature.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.podut.dataagregate.feature.profile.ProfileScreen

const val PROFILE_ROUTE = "settings" // Keep "settings" as per original MainActivity

fun NavGraphBuilder.profileScreen(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    composable(PROFILE_ROUTE) {
        ProfileScreen(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
    }
}
