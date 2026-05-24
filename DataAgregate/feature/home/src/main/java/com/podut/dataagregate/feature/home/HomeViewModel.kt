package com.podut.dataagregate.feature.home

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podut.dataagregate.core.database.dao.UserProfileDao
import com.podut.dataagregate.core.database.entity.UserProfileEntity
import com.podut.dataagregate.core.domain.model.RSSIngestRequest
import com.podut.dataagregate.core.network.IngestApiService
import com.podut.dataagregate.core.database.entity.RssSourceEntity
import com.podut.dataagregate.core.database.entity.InterestEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class HomeUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val rssSources: List<RssSourceEntity> = emptyList(),
    val interests: List<InterestEntity> = emptyList(),
    val pendingDeleteInterest: String? = null,
    val pendingDeleteRssUrl: String? = null,
    val interestToRestore: InterestEntity? = null,
    val rssToRestore: RssSourceEntity? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiService: IngestApiService,
    private val profileDao: UserProfileDao,
    @Named("deviceId") private val deviceId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var pendingInterestDeleteJob: Job? = null
    private var pendingRssDeleteJob: Job? = null

    @OptIn(FlowPreview::class)
    fun init() {
        // 1. Sincronizare SERVER (Sursa de Adevăr)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Sincronizăm Interesele
            apiService.getProfile(deviceId).onSuccess { profile ->
                // NOTĂ: Dacă serverul returnează o listă goală, înseamnă că userul nu are interese salvate încă.
                // Nu mai punem default-uri hardcodate aici.
                profile.favoriteCategories.forEach { name ->
                    profileDao.upsertInterest(InterestEntity(name = name, isSelected = true))
                }
            }
// Sincronizăm Sursele RSS
val catsRes = apiService.getCategories(deviceId)
catsRes.onSuccess { categories ->
    val serverSources = categories.flatMap { cat -> 
        cat.feeds.map { it.url }
    }.toSet()

    // Curățăm fantomele DOAR dacă serverul chiar are date (evităm ștergere din cauza răspuns gol)
    if (serverSources.isNotEmpty()) {
        val localSources = _uiState.value.rssSources
        localSources.forEach { local ->
            if (local.url !in serverSources) {
                profileDao.deleteRssSource(local.url)
            }
        }
    }

    // 2. Upsert la sursele de pe server
    categories.forEach { cat ->
        cat.feeds.forEach { feed ->
            profileDao.upsertRssSource(
                RssSourceEntity(
                    url = feed.url,
                    isEnabled = feed.is_active,
                    category = cat.name
                )
            )
        }
    }
}

            
            _uiState.update { it.copy(isLoading = false) }
        }

        // 2. Colectăm din Room pentru UI
        viewModelScope.launch {
            profileDao.getRssSources().collect { sources ->
                _uiState.update { it.copy(rssSources = sources) }
            }
        }

        viewModelScope.launch {
            profileDao.getInterests().collect { interests ->
                _uiState.update { it.copy(interests = interests) }
            }
        }

        // BACKGROUND SYNC: Sincronizăm modificările locale către server
        viewModelScope.launch {
            profileDao.getInterests()
                .debounce(2000)
                .distinctUntilChanged()
                .collect { interests ->
                    val selectedNames = interests.filter { it.isSelected }.map { it.name }
                    if (selectedNames.isNotEmpty()) {
                        runCatching { apiService.syncProfile(deviceId, selectedNames) }
                    }
                }
        }
    }

    fun addNewRssSource(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || !Patterns.WEB_URL.matcher(trimmed).matches()) {
            _uiState.update { it.copy(message = "URL invalid!", isError = true) }
            return
        }
        
        if (_uiState.value.rssSources.any { it.url == trimmed }) {
            _uiState.update { it.copy(message = "Sursa există deja!", isError = true) }
            return
        }
        
        viewModelScope.launch {
            val newSource = RssSourceEntity(
                url = trimmed,
                isEnabled = true,
                isHealthy = true,
                category = "My Sources"
            )
            profileDao.upsertRssSource(newSource)
            _uiState.update { it.copy(message = "Sursă adăugată local!", isError = false) }

            launch {
                val isHealthy = apiService.checkRssHealth(trimmed)
                if (!isHealthy) {
                    profileDao.upsertRssSource(newSource.copy(isHealthy = false))
                }
                runCatching { apiService.addFeed("My Sources", trimmed, deviceId) }
            }
        }
    }

    fun toggleRssSource(source: RssSourceEntity) {
        viewModelScope.launch {
            val updated = source.copy(isEnabled = !source.isEnabled)
            profileDao.upsertRssSource(updated)
            launch {
                runCatching { apiService.toggleFeed(source.category, source.url, updated.isEnabled, deviceId) }
            }
        }
    }

    fun addNewDataType(type: String) {
        val trimmed = type.trim()
        if (trimmed.isBlank() || _uiState.value.interests.any { it.name == trimmed }) return
        viewModelScope.launch {
            profileDao.upsertInterest(InterestEntity(name = trimmed, isSelected = true))
            launch {
                runCatching { 
                    apiService.createCategory(
                        com.podut.dataagregate.core.domain.model.CategoryCreate(trimmed, emptyList(), deviceId)
                    ) 
                }
                runCatching { 
                    apiService.ingestRss(
                        com.podut.dataagregate.core.domain.model.RSSIngestRequest(urls = emptyList(), category = trimmed, deviceId = deviceId)
                    ) 
                }
            }
        }
    }

    fun toggleInterest(interest: InterestEntity) {
        viewModelScope.launch {
            val updated = interest.copy(isSelected = !interest.isSelected)
            profileDao.upsertInterest(updated)
        }
    }

    fun scheduleDeleteInterest(name: String) {
        pendingInterestDeleteJob?.cancel()
        val interest = _uiState.value.interests.find { it.name == name }
        _uiState.update { it.copy(
            pendingDeleteInterest = name,
            interestToRestore = interest
        ) }
        viewModelScope.launch { profileDao.deleteInterest(name) }
        
        pendingInterestDeleteJob = viewModelScope.launch {
            delay(3_000)
            _uiState.update { it.copy(pendingDeleteInterest = null, interestToRestore = null) }
        }
    }

    fun undoDeleteInterest() {
        pendingInterestDeleteJob?.cancel()
        val toRestore = _uiState.value.interestToRestore
        if (toRestore != null) {
            viewModelScope.launch {
                profileDao.upsertInterest(toRestore)
            }
        }
        _uiState.update { it.copy(pendingDeleteInterest = null, interestToRestore = null) }
    }

    fun removeDataType(name: String) = scheduleDeleteInterest(name)

    fun scheduleDeleteRssSource(source: RssSourceEntity) {
        pendingRssDeleteJob?.cancel()
        _uiState.update { it.copy(
            pendingDeleteRssUrl = source.url,
            rssToRestore = source
        ) }
        viewModelScope.launch { 
            profileDao.deleteRssSource(source.url) 
            
            pendingRssDeleteJob = viewModelScope.launch {
                delay(3_000)
                runCatching { apiService.removeFeed(source.category, source.url, deviceId) }
                _uiState.update { it.copy(pendingDeleteRssUrl = null, rssToRestore = null) }
            }
        }
    }

    fun undoDeleteRssSource() {
        pendingRssDeleteJob?.cancel()
        val toRestore = _uiState.value.rssToRestore
        if (toRestore != null) {
            viewModelScope.launch { profileDao.upsertRssSource(toRestore) }
        }
        _uiState.update { it.copy(pendingDeleteRssUrl = null, rssToRestore = null) }
    }

    fun removeRssSource(source: RssSourceEntity) = scheduleDeleteRssSource(source)

    fun saveConfiguration(category: String, date: Long?, hour: Int, minute: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val interval = hour.coerceAtLeast(1)
            runCatching { apiService.updateScheduler(interval) }

            val enabledUrls = _uiState.value.rssSources.filter { it.isEnabled }.map { it.url }
            if (enabledUrls.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, message = "Selectați cel puțin o sursă RSS!", isError = true) }
                return@launch
            }
            
            val request = RSSIngestRequest(urls = enabledUrls, category = category, deviceId = deviceId)
            apiService.ingestRss(request).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, message = "Sincronizare pornită!", isError = false) } },
                onFailure = { _uiState.update { it.copy(isLoading = false, message = "Eroare: ${it.message}", isError = true) } }
            )
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null, isError = false) }
    }
}
