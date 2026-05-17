package com.podut.dataagregate.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podut.dataagregate.core.domain.model.CategoryCreate
import com.podut.dataagregate.core.domain.model.FeedCategory
import com.podut.dataagregate.core.domain.model.FeedConfig
import com.podut.dataagregate.core.domain.model.SerpConfig
import com.podut.dataagregate.core.domain.model.SerpConfigUpdate
import com.podut.dataagregate.core.network.IngestApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class CategoriesUiState(
    val categories: List<FeedCategory> = emptyList(),
    val isLoading: Boolean             = false,
    val error: String?                 = null,
    val successMsg: String?            = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val api: IngestApiService,
    @Named("deviceId") private val deviceId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            api.getCategories(deviceId).fold(
                onSuccess = { cats -> _state.update { it.copy(categories = cats, isLoading = false) } },
                onFailure = { e  -> _state.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun createCategory(name: String, rssUrls: List<String>) {
        viewModelScope.launch {
            api.createCategory(CategoryCreate(name, rssUrls)).fold(
                onSuccess = { load() },
                onFailure = { e -> _state.update { it.copy(error = e.message) } }
            )
        }
    }

    fun deleteCategory(name: String) {
        val prev = _state.value.categories
        _state.update { it.copy(categories = prev.filter { c -> c.name != name }) }
        viewModelScope.launch {
            api.deleteCategory(name).fold(
                onSuccess = { _state.update { it.copy(successMsg = "Deleted '$name'") } },
                onFailure = { e -> _state.update { it.copy(categories = prev, error = e.message) } }
            )
        }
    }

    fun addFeed(category: String, url: String) {
        val prev = _state.value.categories
        _state.update { s ->
            s.copy(categories = s.categories.map { cat ->
                if (cat.name == category && cat.feeds.none { it.url == url })
                    cat.copy(feeds = cat.feeds + FeedConfig(url, true))
                else cat
            })
        }
        viewModelScope.launch {
            api.addFeed(category, url, deviceId).fold(
                onSuccess = { /* starea e deja actualizata */ },
                onFailure = { e -> _state.update { it.copy(categories = prev, error = e.message) } }
            )
        }
    }

    fun toggleFeed(category: String, url: String) {
        val prev = _state.value.categories
        var newState = false
        _state.update { s ->
            s.copy(categories = s.categories.map { cat ->
                if (cat.name == category) {
                    cat.copy(feeds = cat.feeds.map { 
                        if (it.url == url) {
                            newState = !it.is_active
                            it.copy(is_active = newState)
                        } else it 
                    })
                } else cat
            })
        }
        viewModelScope.launch {
            api.toggleFeed(category, url, newState, deviceId).fold(
                onSuccess = { /* starea e deja actualizata */ },
                onFailure = { e -> _state.update { it.copy(categories = prev, error = e.message) } }
            )
        }
    }

    fun removeFeed(category: String, url: String) {
        val prev = _state.value.categories
        _state.update { s ->
            s.copy(categories = s.categories.map { cat ->
                if (cat.name == category) cat.copy(feeds = cat.feeds.filter { it.url != url })
                else cat
            })
        }
        viewModelScope.launch {
            api.removeFeed(category, url, deviceId).fold(
                onSuccess = { /* starea e deja actualizata */ },
                onFailure = { e -> _state.update { it.copy(categories = prev, error = e.message) } }
            )
        }
    }

    fun updateSerp(category: String, query: String, region: String, enabled: Boolean) {
        val prev = _state.value.categories
        _state.update { s ->
            s.copy(categories = s.categories.map { cat ->
                if (cat.name == category)
                    cat.copy(serp = SerpConfig(query = query, region = region, enabled = enabled))
                else cat
            })
        }
        viewModelScope.launch {
            api.updateSerpConfig(category, SerpConfigUpdate(query, deviceId, region, enabled)).fold(
                onSuccess = { _state.update { it.copy(successMsg = "SERP saved") } },
                onFailure = { e -> _state.update { it.copy(categories = prev, error = e.message) } }
            )
        }
    }

    fun fetchSerp(category: String) {
        viewModelScope.launch {
            api.fetchSerp(category).fold(
                onSuccess = { _state.update { it.copy(successMsg = "SERP fetch started for '$category'") } },
                onFailure = { e -> _state.update { it.copy(error = e.message) } }
            )
        }
    }

    fun clearMsg() = _state.update { it.copy(successMsg = null, error = null) }
}
