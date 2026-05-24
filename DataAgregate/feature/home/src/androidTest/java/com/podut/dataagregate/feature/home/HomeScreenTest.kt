package com.podut.dataagregate.feature.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.podut.dataagregate.core.network.IngestApiService
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val apiService: IngestApiService = mockk()

    @Test
    fun homeScreen_displaysInitialComponents() {
        // Given
        val viewModel = HomeViewModel(apiService)

        // When
        composeTestRule.setContent {
            HomeScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule.onNodeWithText("Configurare Agregator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Surse RSS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Programare Scanare").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interese / Tip Date").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACTIVEAZĂ AGREGATORUL").assertIsDisplayed()
    }

    @Test
    fun homeScreen_buttonDisabled_whenNoRssSelected() {
        // Given
        val viewModel = HomeViewModel(apiService)

        // When
        composeTestRule.setContent {
            HomeScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule.onNodeWithText("ACTIVEAZĂ AGREGATORUL").assertIsNotEnabled()
    }
}
