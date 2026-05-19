package com.clearpath.xray_compose.data

import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    val ACTIVE_SUB_ID = stringPreferencesKey("active_sub_id")
    val ACTIVE_ENGINE_SETTING_ID = stringPreferencesKey("active_engine_setting_id")
}