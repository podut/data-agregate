package com.podut.dataagregate.feature.home

import app.cash.turbine.test
import com.podut.dataagregate.core.network.IngestApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private val apiService: IngestApiService = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addNewRssSource when healthy should add source to list`() = runTest {
        // Given
        val url = "https://test.com/rss"
        coEvery { apiService.checkRssHealth(url) } returns true

        // When
        viewModel.addNewRssSource(url)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.rssSources.any { it.url == url })
            assertEquals("Sursă RSS validată și adăugată!", state.message)
        }
    }

    @Test
    fun `addNewRssSource when unhealthy should show error`() = runTest {
        // Given
        val url = "https://invalid.com/rss"
        coEvery { apiService.checkRssHealth(url) } returns false

        // When
        viewModel.addNewRssSource(url)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isError)
            assertEquals("Sursa RSS nu a putut fi validată.", state.message)
        }
    }

    @Test
    fun `saveConfiguration should call api and show success`() = runTest {
        // Given
        val url = "https://test.com/rss"
        coEvery { apiService.ingestRss(any()) } returns Result.success("OK")

        // When
        viewModel.saveConfiguration(url, "AI", null, 9, 0)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Configurație salvată!", state.message)
            assertEquals(false, state.isLoading)
        }
    }
}
