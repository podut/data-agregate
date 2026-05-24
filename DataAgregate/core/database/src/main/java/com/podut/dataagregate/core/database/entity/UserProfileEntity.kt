package com.podut.dataagregate.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UserProfile entity inspired by 'ebib_cititor' design from Database documentation.
 * Stores device-specific ID and personalization preferences.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val deviceId: String,
    val name: String = "Reader",
    val favoriteCategories: String = "AI,Dev,Tools", // Comma-separated list
    val interestsScore: Int = 100,
    val lastActive: Long = System.currentTimeMillis(),
    val language: String = "en"
)
