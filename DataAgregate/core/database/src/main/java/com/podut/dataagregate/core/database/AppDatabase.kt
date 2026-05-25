package com.podut.dataagregate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.podut.dataagregate.core.database.dao.ArticleDao
import com.podut.dataagregate.core.database.dao.SavedArticleDao
import com.podut.dataagregate.core.database.dao.UserProfileDao
import com.podut.dataagregate.core.database.entity.ArticleEntity
import com.podut.dataagregate.core.database.entity.SavedArticleEntity
import com.podut.dataagregate.core.database.entity.UserProfileEntity
import com.podut.dataagregate.core.database.entity.RssSourceEntity
import com.podut.dataagregate.core.database.entity.InterestEntity

@Database(
    entities     = [
        ArticleEntity::class, 
        SavedArticleEntity::class, 
        UserProfileEntity::class,
        RssSourceEntity::class,
        InterestEntity::class
    ],
    version      = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun savedArticleDao(): SavedArticleDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rss_articles ADD COLUMN source TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE rss_articles ADD COLUMN score INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN language TEXT NOT NULL DEFAULT 'en'")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profile (
                        deviceId           TEXT    NOT NULL PRIMARY KEY,
                        name               TEXT    NOT NULL,
                        favoriteCategories TEXT    NOT NULL,
                        interestsScore     INTEGER NOT NULL,
                        lastActive         INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_articles (
                        link        TEXT    NOT NULL PRIMARY KEY,
                        title       TEXT    NOT NULL,
                        summary     TEXT    NOT NULL,
                        image       TEXT,
                        source      TEXT    NOT NULL,
                        category    TEXT    NOT NULL,
                        score       INTEGER NOT NULL,
                        publishDate TEXT,
                        savedAt     INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
