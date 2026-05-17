package com.podut.dataagregate.feature.category_articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podut.dataagregate.core.database.dao.SavedArticleDao
import com.podut.dataagregate.core.database.entity.toSavedEntity
import com.podut.dataagregate.core.domain.model.TechNews
import com.podut.dataagregate.core.network.IngestApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryArticlesUiState(
    val category: String          = "",
    val articles: List<TechNews>  = emptyList(),
    val savedLinks: Set<String>   = emptySet(),
    val isLoading: Boolean        = false,
    val error: String?            = null
)

@HiltViewModel
class CategoryArticlesViewModel @Inject constructor(
    private val api: IngestApiService,
    private val savedDao: SavedArticleDao
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryArticlesUiState())
    val state = _state.asStateFlow()

    init {
        savedDao.getSavedLinks()
            .onEach { links -> _state.update { it.copy(savedLinks = links.toSet()) } }
            .launchIn(viewModelScope)
    }

    fun load(category: String) {
        if (_state.value.category == category && _state.value.articles.isNotEmpty()) return
        _state.update { it.copy(category = category, isLoading = true, error = null) }
        viewModelScope.launch {
            val byCategory = api.getArticlesByCategory(category).getOrNull().orEmpty()
            val articles = if (byCategory.isNotEmpty()) {
                byCategory
            } else {
                api.getArticlesByTag(category).getOrNull().orEmpty()
            }
            _state.update { it.copy(articles = articles, isLoading = false) }
        }
    }

    fun toggleSave(article: TechNews) {
        viewModelScope.launch {
            if (savedDao.exists(article.link) > 0) {
                savedDao.delete(article.link)
            } else {
                savedDao.insert(article.toSavedEntity())
            }
        }
    }
}
