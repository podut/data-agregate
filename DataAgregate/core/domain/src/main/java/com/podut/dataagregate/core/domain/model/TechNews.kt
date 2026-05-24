package com.podut.dataagregate.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TechNews(
    val title: String,
    val summary: String,
    val image: String? = null,
    val link: String,
    val source: String,
    val category: String,
    val score: Int = 0,
    val publish_date: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class DigestResponse(
    val digest_date: String,
    val news: List<TechNews>
)
