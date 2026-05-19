package com.clearpath.xray_compose.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.clearpath.xray_compose.R
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Serializable
data object ProfileList : NavKey

@Serializable
data class ProfileListShare(val id: String) : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data class ProfileEdit(val id: String) : NavKey

@Serializable
data class ProfileCreate(val templateId: String) : NavKey

@Serializable
data object SettingsInbound : NavKey

@Serializable
data object SettingsDns : NavKey

@Serializable
data object SettingsRouting : NavKey

@Serializable
data class SettingsRule(val id: String) : NavKey

@Serializable
data object Logcat : NavKey

data class NavBarItem(
    val icon: Int,
    val description: String
)


val TOP_LEVEL_ROUTES = mapOf<NavKey, NavBarItem>(
    Home to NavBarItem(
        icon = R.drawable.ic_home,
        description = "Home"
    ),
    ProfileList to NavBarItem(
        icon = R.drawable.ic_view_list,
        description = "Profiles"
    ),
    Settings to NavBarItem(
        icon = R.drawable.ic_settings,
        description = "Settings"
    )
)

fun NavKey.toContentKey() = this.toString()