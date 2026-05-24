package com.podut.dataagregate.core.database.dao

import androidx.room.*
import com.podut.dataagregate.core.database.entity.SavedArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedArticleDao {

    @Query("SELECT * FROM saved_articles ORDER BY savedAt DESC")
    fun getAll(): Flow<List<SavedArticleEntity>>

    @Query("SELECT link FROM saved_articles")
    fun getSavedLinks(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: SavedArticleEntity)

    @Query("DELETE FROM saved_articles WHERE link = :link")
    suspend fun delete(link: String)

    @Query("SELECT COUNT(*) FROM saved_articles WHERE link = :link")
    suspend fun exists(link: String): Int
}
