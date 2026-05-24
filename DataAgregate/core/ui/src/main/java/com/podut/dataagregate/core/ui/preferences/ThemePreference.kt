package com.podut.dataagregate.core.ui.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

@Singleton
class ThemePreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_DARK_THEME] ?: true }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }
}
