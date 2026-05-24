package com.podut.dataagregate.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SerpConfig(
    val query: String?,
    val region: String = "us",
    val enabled: Boolean = false
)

@Serializable
data class FeedConfig(
    val url: String,
    val is_active: Boolean
)

@Serializable
data class FeedCategory(
    val name: String,
    val feeds: List<FeedConfig>,
    val article_count: Int,
    val serp: SerpConfig
)

@Serializable
data class CategoryCreate(
    val name: String,
    val rss_urls: List<String> = emptyList(),
    val deviceId: String? = null
)

@Serializable
data class RssFeedRequest(val url: String, val deviceId: String? = null, val is_active: Boolean? = null)

@Serializable
data class SerpConfigUpdate(
    val query: String,
    val deviceId: String? = null,
    val region: String = "us",
    val enabled: Boolean = true
)

@Serializable
data class UserProfileSync(
    val deviceId: String,
    val favoriteCategories: List<String> = emptyList(),
    val name: String = "Reader",
    val language: String = "en"
)
