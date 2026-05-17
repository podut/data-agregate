package com.podut.dataagregate.feature.news_feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podut.dataagregate.core.database.dao.SavedArticleDao
import com.podut.dataagregate.core.database.dao.UserProfileDao
import com.podut.dataagregate.core.database.entity.UserProfileEntity
import com.podut.dataagregate.core.database.entity.toSavedEntity
import com.podut.dataagregate.core.domain.model.TechNews
import com.podut.dataagregate.core.network.IngestApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 10

data class NewsFeedUiState(
    val articles: List<TechNews>     = emptyList(),
    val heroArticles: List<TechNews> = emptyList(),
    val digestDate: String           = "",
    val selectedCategory: String     = "All",
    val searchQuery: String          = "",
    val savedLinks: Set<String>      = emptySet(),
    val userProfile: UserProfileEntity? = null,
    val isLoading: Boolean           = false,
    val hasMore: Boolean             = false,
    val error: String?               = null,
    val lastSyncTime: String?        = null
)

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
    private val apiService: IngestApiService,
    private val savedDao: SavedArticleDao,
    private val profileDao: UserProfileDao,
    @Named("deviceId") private val deviceId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsFeedUiState())
    val uiState = _uiState.asStateFlow()

    private var rawArticles: List<TechNews> = emptyList()  // scoruri originale din server, niciodata modificate
    private var allArticles: List<TechNews> = emptyList()  // sortate/boosted, recalculate din rawArticles
    private var shownCount = 0

    // Link-uri pentru care am trimis deja mark-seen in sesiunea curenta — evitam request-uri duplicate
    private val sentSeen = mutableSetOf<String>()

    init {
        savedDao.getSavedLinks()
            .onEach { links -> _uiState.update { it.copy(savedLinks = links.toSet()) } }
            .launchIn(viewModelScope)

        profileDao.getProfile(deviceId)
            .onEach { profile ->
                if (profile == null) {
                    profileDao.updateProfile(UserProfileEntity(deviceId = deviceId))
                } else {
                    _uiState.update { it.copy(userProfile = profile) }
                    applySmartSorting()
                }
            }
            .launchIn(viewModelScope)

        loadDigest()
    }

    fun initProfile() { /* pastrat pentru compatibilitate cu LaunchedEffect */ }

    fun refreshFeed() {
        rawArticles = emptyList()
        allArticles = emptyList()
        shownCount  = 0
        sentSeen.clear()
        loadDigest()
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        shownCount = PAGE_SIZE
        pushVisible()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        shownCount = PAGE_SIZE
        pushVisible()
    }

    fun loadMore() {
        shownCount += PAGE_SIZE
        pushVisible()
    }

    fun toggleSave(link: String) {
        val article = allArticles.find { it.link == link } ?: return
        viewModelScope.launch {
            if (savedDao.exists(link) > 0) {
                savedDao.delete(link)
            } else {
                savedDao.insert(article.toSavedEntity())
            }
        }
    }

    private fun applySmartSorting() {
        val favorites = _uiState.value.userProfile?.favoriteCategories?.split(",") ?: emptyList()
        // Recalculeaza mereu din rawArticles — niciodata din allArticles (evita acumulare boost)
        allArticles = if (favorites.isEmpty()) {
            rawArticles
        } else {
            rawArticles.map { news ->
                val isFavorite = favorites.any { it.equals(news.category, ignoreCase = true) }
                val boost = if (isFavorite) 50 else 0
                news.copy(score = (news.score ?: 0) + boost)
            }.sortedByDescending { it.score }
        }
        pushVisible()
    }

    private fun pushVisible() {
        val cat = _uiState.value.selectedCategory
        val q = _uiState.value.searchQuery.trim()
        val heroLinks = _uiState.value.heroArticles.map { it.link }.toSet()

        // Excludem mereu ce e deja în Hero pentru a evita redundanța
        val filtered = allArticles.filter { it.link !in heroLinks }
            .let { list ->
                if (cat == "All") list
                else list.filter { it.category.equals(cat, ignoreCase = true) }
            }
            .let { list ->
                if (q.isBlank()) list
                else list.filter {
                    it.title.contains(q, ignoreCase = true) ||
                    it.summary.contains(q, ignoreCase = true) ||
                    it.source.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true)
                }
            }

        val shown = filtered.take(shownCount)
        _uiState.update { it.copy(articles = shown, hasMore = shown.size < filtered.size) }
        markVisibleAsSeen(shown.map { it.link } + heroLinks)
    }

    private fun markVisibleAsSeen(visibleLinks: List<String>) {
        val newLinks = visibleLinks.filter { it.isNotBlank() && it !in sentSeen }
        if (newLinks.isEmpty()) return
        sentSeen.addAll(newLinks)
        viewModelScope.launch {
            runCatching { apiService.markSeen(deviceId, newLinks) }
        }
    }

    private fun loadDigest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            apiService.getDigest(deviceId = deviceId).fold(
                onSuccess = { digest ->
                    rawArticles = digest.news  // pastreaza scorurile originale
                    applySmartSorting()        // calculeaza allArticles din rawArticles
                    shownCount  = PAGE_SIZE.coerceAtMost(allArticles.size)
                    val syncTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date())
                    
                    val heroList = allArticles.take(5)
                    val remainingArticles = allArticles.drop(5)
                    
                    val visibleArticles = remainingArticles.take(shownCount)
                    _uiState.update {
                        it.copy(
                            articles      = visibleArticles,
                            heroArticles  = heroList,
                            digestDate    = digest.digest_date,
                            isLoading     = false,
                            hasMore       = remainingArticles.size > shownCount,
                            lastSyncTime  = syncTime
                        )
                    }
                    markVisibleAsSeen(heroList.map { it.link } + visibleArticles.map { it.link })
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }
}
