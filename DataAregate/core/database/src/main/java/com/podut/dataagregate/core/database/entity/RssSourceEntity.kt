package com.podut.dataagregate.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss_sources")
data class RssSourceEntity(
    @PrimaryKey val url: String,
    val isEnabled: Boolean = true,
    val isHealthy: Boolean = true,
    val category: String = "My Sources"
)
