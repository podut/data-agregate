package com.podut.dataagregate.di

import android.content.Context
import android.provider.Settings
import com.podut.dataagregate.BuildConfig
import com.podut.dataagregate.theme.ThemePreference
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
    fun provideBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Named("deviceId")
    fun provideDeviceId(@ApplicationContext context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    @Provides
    @Singleton
    fun provideThemePreference(@ApplicationContext context: Context): ThemePreference =
        ThemePreference(context)
}
