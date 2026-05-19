package com.clearpath.xray_compose.ui.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.clearpath.xray_compose.ui.navigation.bottomsheet.BottomSheetSceneStrategy
import com.clearpath.xray_compose.ui.navigation.sharedviewmodel.SharedViewModelStoreNavEntryDecorator
import com.clearpath.xray_compose.ui.screen.HomeScreen
import com.clearpath.xray_compose.ui.screen.LogcatScreen
import com.clearpath.xray_compose.ui.screen.ProfileListScreen
import com.clearpath.xray_compose.ui.screen.SettingsScreen
import com.clearpath.xray_compose.ui.screen.profile.ProfileCreateScreen
import com.clearpath.xray_compose.ui.screen.profile.ProfileEditScreen
import com.clearpath.xray_compose.ui.screen.profile.ProfileListShareScreen
import com.clearpath.xray_compose.ui.screen.settings.SettingsDnsScreen
import com.clearpath.xray_compose.ui.screen.settings.SettingsInboundScreen
import com.clearpath.xray_compose.ui.screen.settings.SettingsRoutingScreen
import com.clearpath.xray_compose.ui.screen.settings.SettingsRuleScreen
import com.clearpath.xray_compose.ui.screen.settings.SettingsSubEditorScreen
import com.clearpath.xray_compose.ui.screen.settings.SettingsSubScreen

fun EntryProviderScope<NavKey>.homeSection() {
    entry<Home>(
        clazzContentKey = { key -> key.toContentKey() },
    ) {
        HomeScreen()
    }
}

fun EntryProviderScope<NavKey>.profileSection() {
    entry<ProfileList>(
        clazzContentKey = { key -> key.toContentKey() },
    ) {
        ProfileListScreen()
    }

    entry<ProfileListShare>(
        metadata = {
            BottomSheetSceneStrategy.bottomSheet() +
                    SharedViewModelStoreNavEntryDecorator.parent(
                        ProfileList.toContentKey()
                    )
        }
    ) { args ->
        ProfileListShareScreen(args.id)
    }

    entry<ProfileEdit>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            ProfileList.toContentKey()
        )
    ) { args ->
        ProfileEditScreen(args.id)
    }

    entry<ProfileCreate>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            ProfileList.toContentKey()
        )
    ) { args ->
        ProfileCreateScreen(templateId = args.templateId)
    }
}

fun EntryProviderScope<NavKey>.settingsSection() {
    entry<Settings>(
        clazzContentKey = { key -> key.toContentKey() },
    ) {
        SettingsScreen()
    }
    entry<SettingsInbound>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            Settings.toContentKey()
        )
    ) {
        SettingsInboundScreen()
    }
    entry<SettingsDns>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            Settings.toContentKey()
        )
    ) {
        SettingsDnsScreen()
    }
    entry<SettingsRouting>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            Settings.toContentKey()
        )
    ) {
        SettingsRoutingScreen()
    }
    entry<SettingsRule>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            SettingsRouting.toContentKey()
        )
    ) { args ->
        SettingsRuleScreen(args.id)
    }
    entry<SettingsSub>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            Settings.toContentKey()
        )
    ) {
        SettingsSubScreen()
    }
    entry<SettingsSubEditor>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            SettingsSub.toContentKey()
        )
    ) { args ->
        SettingsSubEditorScreen(args.id)
    }
    entry<Logcat>(
        metadata = SharedViewModelStoreNavEntryDecorator.parent(
            Settings.toContentKey()
        )
    ) {
        LogcatScreen()
    }
}