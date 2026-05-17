package com.podut.dataagregate.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.podut.dataagregate.core.domain.model.TechNews

@Entity(tableName = "saved_articles")
data class SavedArticleEntity(
    @PrimaryKey val link: String,
    val title: String,
    val summary: String,
    val image: String?,
    val source: String,
    val category: String,
    val score: Int,
    val publishDate: String?,
    val savedAt: Long = System.currentTimeMillis()
)

fun SavedArticleEntity.toTechNews() = TechNews(
    title        = title,
    summary      = summary,
    image        = image,
    link         = link,
    source       = source,
    category     = category,
    score        = score,
    publish_date = publishDate
)

fun TechNews.toSavedEntity() = SavedArticleEntity(
    link        = link,
    title       = title,
    summary     = summary,
    image       = image,
    source      = source,
    category    = category,
    score       = score,
    publishDate = publish_date,
    savedAt     = System.currentTimeMillis()
)
