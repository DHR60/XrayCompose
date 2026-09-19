package com.clearpath.xray_compose.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer

private var INSTANCE: DataStore<Preferences>? = null

val Context.preferencesDataStore: DataStore<Preferences>
    get() = INSTANCE ?: synchronized(MultiProcessDataStoreFactory::class.java) {
        INSTANCE ?: MultiProcessDataStoreFactory.create(
            serializer = PreferencesFileSerializer,
            produceFile = {
                this.applicationContext.dataStoreFile("config.preferences_pb")
            }
        ).also { INSTANCE = it }
    }