package com.podut.dataagregate.feature.news_feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podut.dataagregate.core.database.dao.ArticleDao
import com.podut.dataagregate.core.database.dao.SavedArticleDao
import com.podut.dataagregate.core.database.dao.UserProfileDao
import com.podut.dataagregate.core.database.entity.ArticleEntity
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
    val articles: List<TechNews>        = emptyList(),
    val heroArticles: List<TechNews>    = emptyList(),
    val trendingArticles: List<TechNews> = emptyList(),
    val digestDate: String              = "",
    val selectedCategory: String        = "All",
    val searchQuery: String             = "",
    val savedLinks: Set<String>         = emptySet(),
    val userProfile: UserProfileEntity? = null,
    val isLoading: Boolean              = false,
    val isRefreshing: Boolean           = false,
    val hasMore: Boolean                = false,
    val error: String?                  = null,
    val lastSyncTime: String?           = null,
    val seenLinks: Set<String>          = emptySet(),
    val allCaughtUp: Boolean            = false,
    val newArticlesCount: Int           = 0,
    val refreshMessage: String?         = null
)

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
    private val apiService: IngestApiService,
    private val articleDao: ArticleDao,
    private val savedDao: SavedArticleDao,
    private val profileDao: UserProfileDao,
    @Named("deviceId") private val deviceId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsFeedUiState())
    val uiState = _uiState.asStateFlow()

    private var rawArticles: List<TechNews> = emptyList()
    private var allArticles: List<TechNews> = emptyList()
    private var shownCount = 0
    private var trendingLoading = false

    // Articole trimise deja la server ca "seen" — evităm request-uri duplicate
    private val sentToServer = mutableSetOf<String>()
    // Articole pe care utilizatorul le-a deschis/văzut în sesiunea curentă
    private val localSeenLinks = mutableSetOf<String>()

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

    fun initProfile() { /* compatibilitate LaunchedEffect */ }

    fun refreshFeed() {
        if (_uiState.value.isRefreshing) return
        val currentlyShown = (_uiState.value.articles.map { it.link } + _uiState.value.heroArticles.map { it.link })
            .filter { it.isNotBlank() }
        val seenToFlush = (localSeenLinks + currentlyShown).distinct()
        rawArticles    = emptyList()
        allArticles    = emptyList()
        shownCount     = 0
        trendingLoading = false
        localSeenLinks.clear()
        sentToServer.clear()
        _uiState.update { it.copy(
            seenLinks        = emptySet(),
            trendingArticles = emptyList(),
            allCaughtUp      = false,
            newArticlesCount = 0,
            isRefreshing     = true,
            refreshMessage   = null
        ) }
        // Mark seen FIRST, then fetch — avoids race where digest returns same articles
        viewModelScope.launch {
            if (seenToFlush.isNotEmpty()) {
                runCatching { apiService.markSeen(deviceId, seenToFlush) }
            }
            doLoadDigest(excludeSeen = true)
        }
    }

    fun onArticlesVisible(links: List<String>) {
        val newLinks = links.filter { it.isNotBlank() && it !in localSeenLinks }
        if (newLinks.isEmpty()) return
        localSeenLinks.addAll(newLinks)
        _uiState.update { it.copy(seenLinks = localSeenLinks.toSet()) }
    }

    fun clearRefreshMessage() {
        _uiState.update { it.copy(refreshMessage = null) }
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

    /**
     * Apelat când utilizatorul deschide un articol.
     * Marchează articolul ca văzut local ȘI trimite la server.
     */
    fun onArticleOpened(link: String) {
        if (link.isBlank()) return
        localSeenLinks.add(link)
        _uiState.update { it.copy(
            seenLinks        = localSeenLinks.toSet(),
            trendingArticles = it.trendingArticles.filter { a -> a.link !in localSeenLinks }
        ) }
        pushVisible()
        sendSeenToServer(listOf(link))
    }

    fun toggleSave(link: String) {
        val article = allArticles.find { it.link == link } ?: return
        viewModelScope.launch {
            if (savedDao.exists(link) > 0) savedDao.delete(link)
            else savedDao.insert(article.toSavedEntity())
        }
    }

    private fun applySmartSorting() {
        val favorites = _uiState.value.userProfile?.favoriteCategories?.split(",") ?: emptyList()
        allArticles = if (favorites.isEmpty()) {
            rawArticles
        } else {
            rawArticles.map { news ->
                val boost = if (favorites.any { it.equals(news.category, ignoreCase = true) }) 50 else 0
                news.copy(score = (news.score ?: 0) + boost)
            }.sortedByDescending { it.score }
        }
        pushVisible()
    }

    private fun pushVisible() {
        val cat       = _uiState.value.selectedCategory
        val q         = _uiState.value.searchQuery.trim()
        val favorites = _uiState.value.userProfile
            ?.favoriteCategories
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: emptyList()

        val pool = allArticles
            .filter { it.link !in localSeenLinks }
            .let { list ->
                // Filtrare strictă doar când userul alege explicit o categorie
                if (cat != "All") list.filter { it.category.equals(cat, ignoreCase = true) }
                else list
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

        // Recalculăm hero-urile doar la load inițial (shownCount == PAGE_SIZE)
        val heroList  = if (shownCount <= PAGE_SIZE) pool.take(5)
                        else _uiState.value.heroArticles
        val heroLinks = heroList.map { it.link }.toSet()

        val remaining   = pool.filter { it.link !in heroLinks }
        val shown       = remaining.take(shownCount)
        val allCaughtUp = shown.isEmpty() && rawArticles.isNotEmpty() && q.isBlank()

        _uiState.update { it.copy(
            heroArticles = heroList,
            articles     = shown,
            hasMore      = shown.size < remaining.size,
            allCaughtUp  = allCaughtUp
        ) }

        if (allCaughtUp && _uiState.value.trendingArticles.isEmpty() && rawArticles.isNotEmpty()) {
            loadTrending()
        }
    }

    private fun loadTrending() {
        if (trendingLoading) return
        trendingLoading = true
        val categories = _uiState.value.userProfile
            ?.favoriteCategories
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?.joinToString(",")
        viewModelScope.launch {
            apiService.getTrending(deviceId = deviceId, categories = categories).onSuccess { trending ->
                val heroLinks = _uiState.value.heroArticles.map { it.link }.toSet()
                val mainLinks = _uiState.value.articles.map { it.link }.toSet()
                val filtered = trending.filter {
                    it.link !in heroLinks && it.link !in mainLinks && it.link !in localSeenLinks
                }
                _uiState.update { it.copy(trendingArticles = filtered) }
            }
            trendingLoading = false
        }
    }

    private fun sendSeenToServer(links: List<String>) {
        val newLinks = links.filter { it.isNotBlank() && it !in sentToServer }
        if (newLinks.isEmpty()) return
        sentToServer.addAll(newLinks)
        viewModelScope.launch {
            runCatching { apiService.markSeen(deviceId, newLinks) }
        }
    }

    private fun loadDigest(excludeSeen: Boolean = false) {
        viewModelScope.launch { doLoadDigest(excludeSeen) }
    }

    private suspend fun doLoadDigest(excludeSeen: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        apiService.getDigest(deviceId = deviceId, excludeSeen = excludeSeen).fold(
            onSuccess = { digest ->
                // Salvăm în Room pentru offline cache
                val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L  // 7 zile
                articleDao.deleteOlderThan(cutoff)
                articleDao.insertArticles(digest.news.map { it.toEntity() })

                rawArticles = digest.news
                shownCount  = PAGE_SIZE
                applySmartSorting()

                val syncTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                _uiState.update { it.copy(
                    digestDate       = digest.digest_date,
                    isLoading        = false,
                    isRefreshing     = false,
                    lastSyncTime     = syncTime,
                    newArticlesCount = 0,
                    refreshMessage   = null
                ) }
            },
            onFailure = { err ->
                // Fallback: încarcă din Room DB cache
                val cached = articleDao.getRecentArticles()
                if (cached.isNotEmpty()) {
                    rawArticles = cached.map { it.toTechNews() }
                    shownCount  = PAGE_SIZE
                    applySmartSorting()
                }
                _uiState.update { it.copy(
                    isLoading      = false,
                    isRefreshing   = false,
                    error          = if (cached.isEmpty()) err.message else null,
                    refreshMessage = if (cached.isNotEmpty()) "Afișând știri salvate local." else "Nu s-a putut conecta la server."
                ) }
            }
        )
    }

    private fun TechNews.toEntity() = ArticleEntity(
        url         = link,
        title       = title,
        summary     = summary,
        category    = category,
        imageUrl    = image,
        publishedAt = System.currentTimeMillis(),
        source      = source,
        score       = score
    )

    private fun ArticleEntity.toTechNews() = TechNews(
        title        = title,
        summary      = summary,
        image        = imageUrl,
        link         = url,
        source       = source,
        category     = category,
        score        = score,
        publish_date = null
    )
}
