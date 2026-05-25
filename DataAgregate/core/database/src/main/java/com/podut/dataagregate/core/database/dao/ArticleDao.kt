package com.podut.dataagregate.core.database.dao

import androidx.room.*
import com.podut.dataagregate.core.database.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM rss_articles ORDER BY score DESC, publishedAt DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM rss_articles ORDER BY score DESC, publishedAt DESC LIMIT 100")
    suspend fun getRecentArticles(): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM rss_articles WHERE publishedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM rss_articles")
    suspend fun clearAll()
}
