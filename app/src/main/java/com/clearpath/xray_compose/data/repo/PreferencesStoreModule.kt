package com.clearpath.xray_compose.data.repo

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.preferencesDataStore by preferencesDataStore(name = "config.preferences_pb")