package com.clearpath.xray_compose.service.engine.context

import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.data.tempstore.TempStore
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class EngineConfigContextBuilderResult(
    val ecContext: EngineConfigContext?,
    val errors: List<String>,
    val warnings: List<String>,
) {
    val success: Boolean
        get() = errors.isEmpty()
}

@Singleton
class EngineConfigContextBuilder @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val profileRepository: ProfileRepository,
    private val configRepository: ConfigRepository
) {

    suspend fun buildActiveProfile(): EngineConfigContextBuilderResult {
        val activeProfileId = preferencesRepository.getActiveProfileId()
            ?: return EngineConfigContextBuilderResult(
                ecContext = null,
                errors = listOf("No active profile ID found in preferences."),
                warnings = emptyList(),
            )
        val activeProfile =
            profileRepository.getProfileById(activeProfileId) ?: run {
                return EngineConfigContextBuilderResult(
                    ecContext = null,
                    errors = listOf("Active profile with ID $activeProfileId not found."),
                    warnings = emptyList(),
                )
            }

        return buildByProfile(activeProfile)
    }

    suspend fun build(profileId: String): EngineConfigContextBuilderResult {
        val profile = TempStore.consume(profileId)
            ?: profileRepository.getProfileById(profileId)
            ?: run {
                return EngineConfigContextBuilderResult(
                    ecContext = null,
                    errors = listOf("Profile with ID $profileId not found."),
                    warnings = emptyList(),
                )
            }

        return buildByProfile(profile)
    }


    suspend fun buildByProfile(profileModel: ProfileModel): EngineConfigContextBuilderResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val activeEngineSettingId = preferencesRepository.getActiveEngineSettingId()

        val engineSettingList = configRepository.getConfig().engineSettingList

        val activeEngineSetting = if (activeEngineSettingId != null) {
            engineSettingList.find { it.id == activeEngineSettingId } ?: run {
                errors.add("Active engine setting with ID $activeEngineSettingId not found.")
                null
            }
        } else {
            warnings.add("No active engine setting ID found in preferences.")
            null
        } ?: engineSettingList.firstOrNull()
        ?: ConfigEngineItem()

        val allProxiesMap = mapOf(
            profileModel.id to profileModel
        )

        return EngineConfigContextBuilderResult(
            ecContext = EngineConfigContext(
                node = profileModel,
                engineConfig = activeEngineSetting,
                allProxiesMap = allProxiesMap,
                isTunEnabled = !activeEngineSetting.inbound.disableTun,
            ),
            errors = errors,
            warnings = warnings,
        )
    }

    suspend fun quickCheck(): List<String> {
        val errors = mutableListOf<String>()

        val activeProfileId = preferencesRepository.getActiveProfileId()
        if (activeProfileId == null) {
            errors.add("No active profile ID found in preferences.")
            return errors
        }

        val activeProfile = profileRepository.getProfileById(activeProfileId)
        if (activeProfile == null) {
            errors.add("Active profile with ID $activeProfileId not found.")
            return errors
        }

        return errors
    }

    suspend fun isVpnMode(): Boolean {
        return !(configRepository.engineSettingListFlow.firstOrNull()
            ?.firstOrNull()?.inbound?.disableTun
            ?: false)
    }
}