package com.podut.dataagregate.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SchedulerConfig(
    val interval_hours: Int
)
