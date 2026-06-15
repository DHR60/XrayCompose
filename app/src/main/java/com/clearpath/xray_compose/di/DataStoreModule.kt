package com.clearpath.xray_compose.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.clearpath.xray_compose.data.ConfigItem
import com.clearpath.xray_compose.data.repo.configDataStore
import com.clearpath.xray_compose.data.repo.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideConfigDataStore(@ApplicationContext context: Context): DataStore<ConfigItem> {
        return context.configDataStore
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.preferencesDataStore
    }
}
