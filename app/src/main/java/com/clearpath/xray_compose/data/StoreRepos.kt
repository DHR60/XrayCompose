package com.clearpath.xray_compose.data

import android.content.Context
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.data.repo.configRepository
import com.clearpath.xray_compose.data.repo.preferencesRepository
import com.clearpath.xray_compose.data.repo.profileRepository

data class StoreRepos(
    val profileRepo: ProfileRepository,
    val configRepo: ConfigRepository,
    val prefsRepo: PreferencesRepository
) {
    companion object {
        fun getOrBuildSingleton(context: Context) = StoreRepos(
            profileRepo = context.profileRepository,
            configRepo = context.configRepository,
            prefsRepo = context.preferencesRepository,
        )

        fun getSingleton() = StoreRepos(
            profileRepo = profileRepository,
            configRepo = configRepository,
            prefsRepo = preferencesRepository,
        )
    }
}
