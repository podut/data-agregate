package com.podut.dataagregate.core.database.dao

import androidx.room.*
import com.podut.dataagregate.core.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE deviceId = :deviceId")
    fun getProfile(deviceId: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("SELECT favoriteCategories FROM user_profile WHERE deviceId = :deviceId")
    suspend fun getFavoriteCategories(deviceId: String): String?

    // RSS Sources
    @Query("SELECT * FROM rss_sources")
    fun getRssSources(): Flow<List<com.podut.dataagregate.core.database.entity.RssSourceEntity>>

    @Upsert
    suspend fun upsertRssSource(source: com.podut.dataagregate.core.database.entity.RssSourceEntity)

    @Query("DELETE FROM rss_sources WHERE url = :url")
    suspend fun deleteRssSource(url: String)

    // Interests
    @Query("SELECT * FROM interests")
    fun getInterests(): Flow<List<com.podut.dataagregate.core.database.entity.InterestEntity>>

    @Upsert
    suspend fun upsertInterest(interest: com.podut.dataagregate.core.database.entity.InterestEntity)

    @Query("DELETE FROM interests WHERE name = :name")
    suspend fun deleteInterest(name: String)
}
