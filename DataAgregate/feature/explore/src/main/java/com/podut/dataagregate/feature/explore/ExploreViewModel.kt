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

private const val EXPLORE_PAGE_SIZE = 15

data class ExploreUiState(
    val categories: List<String> = emptyList(),
    val visibleCategories: List<String> = emptyList(),
    val featuredByCategory: Map<String, TechNews?> = emptyMap(),
    val hasMore: Boolean = false,
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

    private var shownCount = EXPLORE_PAGE_SIZE

    init {
        viewModelScope.launch {
            api.getSchedulerConfig().onSuccess { config ->
                _uiState.update { it.copy(currentInterval = config.interval_hours) }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileDao.getInterests().collect { allInterests ->
                val selectedCats = allInterests.filter { it.isSelected }.map { it.name }
                shownCount = EXPLORE_PAGE_SIZE
                val visible = selectedCats.take(shownCount)
                _uiState.update {
                    it.copy(
                        categories = selectedCats,
                        visibleCategories = visible,
                        hasMore = selectedCats.size > shownCount,
                        isLoading = false
                    )
                }
                if (visible.isNotEmpty()) loadFeatured(visible)
            }
        }
    }

    fun init() { }

    fun loadMore() {
        shownCount += EXPLORE_PAGE_SIZE
        val all = _uiState.value.categories
        val visible = all.take(shownCount)
        val newCats = visible.filter { it !in _uiState.value.visibleCategories }
        _uiState.update {
            it.copy(
                visibleCategories = visible,
                hasMore = all.size > shownCount
            )
        }
        if (newCats.isNotEmpty()) loadFeatured(newCats)
    }

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
            _uiState.update { state ->
                state.copy(featuredByCategory = state.featuredByCategory + pairs.toMap())
            }
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
