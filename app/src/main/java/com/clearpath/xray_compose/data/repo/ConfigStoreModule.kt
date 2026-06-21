package com.clearpath.xray_compose.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.dataStoreFile
import com.clearpath.xray_compose.data.ConfigItem

private var INSTANCE: DataStore<ConfigItem>? = null

val Context.configDataStore: DataStore<ConfigItem>
    get() = INSTANCE ?: synchronized(MultiProcessDataStoreFactory::class.java) {
        INSTANCE ?: MultiProcessDataStoreFactory.create(
            serializer = ConfigItemSerializer,
            produceFile = { this.applicationContext.dataStoreFile("config.json") }
        ).also { INSTANCE = it }
    }
