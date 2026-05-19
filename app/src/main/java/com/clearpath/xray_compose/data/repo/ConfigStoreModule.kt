package com.clearpath.xray_compose.data.repo

import android.content.Context
import androidx.datastore.dataStore

val Context.configDataStore by dataStore(
    fileName = "config.json",
    serializer = ConfigItemSerializer
)