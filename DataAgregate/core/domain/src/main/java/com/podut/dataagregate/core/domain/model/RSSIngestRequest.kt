package com.podut.dataagregate.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RSSIngestRequest(
    val urls: List<String>,
    val category: String = "General",
    val deviceId: String? = null
)

