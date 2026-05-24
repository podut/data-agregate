package com.podut.dataagregate.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podut.dataagregate.core.database.dao.UserProfileDao
import com.podut.dataagregate.core.database.entity.UserProfileEntity
import com.podut.dataagregate.core.network.IngestApiService
import com.podut.dataagregate.core.ui.preferences.OnboardingPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class ProfileUiState(
    val name: String = "Reader",
    val language: String = "en",
    val favoriteCategories: List<String> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileDao: UserProfileDao,
    private val api: IngestApiService,
    private val onboardingPreference: OnboardingPreference,
    @Named("deviceId") private val deviceId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileDao.getProfile(deviceId).collect { profile ->
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            name = profile.name,
                            language = profile.language,
                            favoriteCategories = profile.favoriteCategories
                                .split(",").map { c -> c.trim() }.filter { c -> c.isNotBlank() },
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun updateProfile(name: String, language: String) {
        viewModelScope.launch {
            val currentCats = profileDao.getFavoriteCategories(deviceId) ?: ""
            val entity = UserProfileEntity(
                deviceId = deviceId,
                name = name,
                language = language,
                favoriteCategories = currentCats,
                lastActive = System.currentTimeMillis()
            )
            profileDao.updateProfile(entity)
            api.syncProfile(
                deviceId = deviceId,
                favoriteCategories = currentCats.split(",").filter { it.isNotBlank() },
                name = name,
                language = language
            )
        }
    }

    fun resetOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            onboardingPreference.resetOnboarding()
            onDone()
        }
    }
}
