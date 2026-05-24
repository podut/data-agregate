package com.podut.dataagregate.di

import android.content.Context
import com.podut.dataagregate.BuildConfig
import com.podut.dataagregate.core.ui.preferences.OnboardingPreference
import com.podut.dataagregate.core.ui.preferences.ThemePreference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Named("baseUrl")
    fun provideBaseUrl(): String {
        val originalUrl = BuildConfig.API_BASE_URL
        val isLocal = originalUrl.startsWith("http://localhost") || 
                      originalUrl.startsWith("http://127.0.0.1") || 
                      originalUrl.contains("192.168.") ||
                      originalUrl.contains("pixcode.go.ro")
                      
        return if (isLocal && isEmulator()) {
            "http://10.0.2.2:8085"
        } else {
            originalUrl
        }
    }

    private fun isEmulator(): Boolean {
        val fingerprint = android.os.Build.FINGERPRINT ?: ""
        val model = android.os.Build.MODEL ?: ""
        val manufacturer = android.os.Build.MANUFACTURER ?: ""
        val brand = android.os.Build.BRAND ?: ""
        val device = android.os.Build.DEVICE ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        
        return (fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || "google_sdk" == product)
    }

    @Provides
    @Named("deviceId")
    @Singleton
    fun provideDeviceId(onboardingPreference: OnboardingPreference): String =
        onboardingPreference.getOrCreateDeviceId()

    @Provides
    @Singleton
    fun provideThemePreference(@ApplicationContext context: Context): ThemePreference =
        ThemePreference(context)
}
