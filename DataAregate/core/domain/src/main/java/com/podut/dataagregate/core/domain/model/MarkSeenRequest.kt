package com.podut.dataagregate.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MarkSeenRequest(
    val deviceId: String,
    val urls: List<String>
)
