package com.podut.dataagregate.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ONBOARDING_ROUTE = "onboarding"

fun NavGraphBuilder.onboardingScreen(onDone: () -> Unit) {
    composable(ONBOARDING_ROUTE) {
        OnboardingScreen(onDone = onDone)
    }
}
