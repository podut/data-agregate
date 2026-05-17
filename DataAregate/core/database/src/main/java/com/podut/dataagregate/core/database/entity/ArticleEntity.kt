package com.podut.dataagregate.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss_articles")
data class ArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val summary: String,
    val category: String,
    val imageUrl: String?,
    val publishedAt: Long
)
