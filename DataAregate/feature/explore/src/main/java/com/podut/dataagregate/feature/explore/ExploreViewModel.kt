package com.podut.dataagregate.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podut.dataagregate.core.database.dao.UserProfileDao
import com.podut.dataagregate.core.domain.model.TechNews
import com.podut.dataagregate.core.network.IngestApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class ExploreUiState(
    val categories: List<String> = emptyList(),
    val featuredByCategory: Map<String, TechNews?> = emptyMap(),
    val isLoading: Boolean = false,
    val showSchedulerDialog: Boolean = false,
    val currentInterval: Int = 1
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val profileDao: UserProfileDao,
    private val api: IngestApiService,
    @Named("deviceId") private val deviceId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load scheduler interval
            api.getSchedulerConfig().onSuccess { config ->
                _uiState.update { it.copy(currentInterval = config.interval_hours) }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileDao.getInterests().collect { allInterests ->
                val selectedCats = allInterests
                    .filter { it.isSelected }
                    .map { it.name }

                _uiState.update {
                    it.copy(
                        categories = selectedCats,
                        isLoading = false
                    )
                }
                if (selectedCats.isNotEmpty()) loadFeatured(selectedCats)
            }
        }
    }

    fun init() { }

    private fun loadFeatured(cats: List<String>) {
        viewModelScope.launch {
            val pairs = cats.map { cat ->
                async {
                    val first = api.getArticlesByTag(cat, limit = 5, offset = 0)
                        .getOrNull()
                        ?.firstOrNull()
                        ?: api.getArticlesByCategory(cat, limit = 1, offset = 0)
                            .getOrNull()
                            ?.firstOrNull()
                    cat to first
                }
            }.awaitAll()
            _uiState.update { it.copy(featuredByCategory = pairs.toMap()) }
        }
    }

    fun toggleSchedulerDialog(show: Boolean) {
        _uiState.update { it.copy(showSchedulerDialog = show) }
    }

    fun updateScheduler(hours: Int) {
        viewModelScope.launch {
            api.updateScheduler(hours).onSuccess {
                _uiState.update { it.copy(currentInterval = hours, showSchedulerDialog = false) }
            }
        }
    }
}
