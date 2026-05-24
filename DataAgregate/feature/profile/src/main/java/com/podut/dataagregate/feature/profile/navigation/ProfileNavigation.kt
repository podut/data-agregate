package com.podut.dataagregate.feature.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.podut.dataagregate.feature.profile.ProfileScreen

const val PROFILE_ROUTE = "settings"

fun NavGraphBuilder.profileScreen(
    onToggleTheme: () -> Unit,
    onResetOnboarding: () -> Unit = {}
) {
    composable(PROFILE_ROUTE) {
        ProfileScreen(
            onToggleTheme     = onToggleTheme,
            onResetOnboarding = onResetOnboarding
        )
    }
}
