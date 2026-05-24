package com.podut.dataagregate.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podut.dataagregate.core.database.dao.SavedArticleDao
import com.podut.dataagregate.core.database.entity.toTechNews
import com.podut.dataagregate.core.domain.model.TechNews
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedUiState(val articles: List<TechNews> = emptyList())

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val savedDao: SavedArticleDao
) : ViewModel() {

    val uiState = savedDao.getAll()
        .map { entities -> SavedUiState(articles = entities.map { it.toTechNews() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState())

    fun remove(link: String) {
        viewModelScope.launch { savedDao.delete(link) }
    }
}
