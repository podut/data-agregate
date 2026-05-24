package com.podut.dataagregate.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podut.dataagregate.core.database.dao.UserProfileDao
import com.podut.dataagregate.core.database.entity.InterestEntity
import com.podut.dataagregate.core.database.entity.RssSourceEntity
import com.podut.dataagregate.core.database.entity.UserProfileEntity
import com.podut.dataagregate.core.domain.model.CategoryCreate
import com.podut.dataagregate.core.network.IngestApiService
import com.podut.dataagregate.core.ui.preferences.OnboardingPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val apiService: IngestApiService,
    private val profileDao: UserProfileDao,
    private val onboardingPreference: OnboardingPreference,
    @Named("deviceId") private val deviceId: String
) : ViewModel() {

    var selectedInterests by mutableStateOf(setOf<String>())
        private set
    var selectedLanguage by mutableStateOf("en")
        private set
    var isSyncing by mutableStateOf(false)
        private set

    fun toggleInterest(interest: String) {
        selectedInterests = if (interest in selectedInterests)
            selectedInterests - interest
        else
            selectedInterests + interest
    }

    fun selectLanguage(lang: String) {
        selectedLanguage = lang
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            isSyncing = true

            // 1. Salvăm profilul local
            profileDao.updateProfile(
                UserProfileEntity(
                    deviceId           = deviceId,
                    favoriteCategories = selectedInterests.joinToString(",").ifEmpty { "AI,Tech" },
                    language           = selectedLanguage
                )
            )

            // 2. Sincronizăm profilul pe server
            runCatching {
                apiService.syncProfile(
                    deviceId           = deviceId,
                    favoriteCategories = selectedInterests.toList(),
                    language           = selectedLanguage
                )
            }

            // 3. Salvăm interesele în tabelul local Room (InterestEntity) — Explore citește de aici
            selectedInterests.forEach { name ->
                profileDao.upsertInterest(InterestEntity(name = name, isSelected = true))
            }

            // 4. Seed local Room DB with default RSS sources for selected interests + language
            val defaultFeeds = DefaultRssFeeds.forInterestsAndLanguage(selectedInterests, selectedLanguage)
            defaultFeeds.forEach { (category, url) ->
                profileDao.upsertRssSource(
                    RssSourceEntity(url = url, isEnabled = true, isHealthy = true, category = category)
                )
            }

            // 5. Register categories + feeds in parallel (best-effort)
            coroutineScope {
                selectedInterests.map { category ->
                    async { runCatching { apiService.createCategory(CategoryCreate(category, emptyList(), deviceId)) } }
                }.awaitAll()
                defaultFeeds.map { (category, url) ->
                    async { runCatching { apiService.addFeed(category, url, deviceId) } }
                }.awaitAll()
            }

            // 6. Trigger sync in background — don't block the user
            launch { runCatching { apiService.syncAll() } }

            onboardingPreference.setOnboardingCompleted()
            isSyncing = false
            onDone()
        }
    }
}
