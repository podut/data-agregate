package com.podut.dataagregate.core.ui.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingStore by preferencesDataStore(name = "onboarding")
private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

@Singleton
class OnboardingPreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)

    val onboardingCompleted: Flow<Boolean> = context.onboardingStore.data
        .map { it[KEY_ONBOARDING_DONE] ?: false }

    fun getOrCreateDeviceId(): String {
        val existing = sharedPrefs.getString("device_id", null)
        if (!existing.isNullOrEmpty()) return existing
        val newId = UUID.randomUUID().toString()
        sharedPrefs.edit().putString("device_id", newId).apply()
        return newId
    }

    suspend fun setOnboardingCompleted() {
        context.onboardingStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun resetOnboarding() {
        context.onboardingStore.edit { it[KEY_ONBOARDING_DONE] = false }
    }
}
