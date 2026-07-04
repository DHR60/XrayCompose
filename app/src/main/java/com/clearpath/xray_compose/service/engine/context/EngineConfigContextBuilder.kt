package com.clearpath.xray_compose.service.engine.context

import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.repo.StoreRepository
import com.clearpath.xray_compose.data.tempstore.TempStore
import kotlinx.coroutines.flow.first
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
    private val storeRepository: StoreRepository,
) {
    suspend fun buildActiveProfile(): EngineConfigContextBuilderResult {
        val activeProfile =
            storeRepository.activeProfileFlow.first() ?: run {
                return EngineConfigContextBuilderResult(
                    ecContext = null,
                    errors = listOf("No active profile found."),
                    warnings = emptyList(),
                )
            }

        return buildByProfile(activeProfile)
    }

    suspend fun build(profileId: String): EngineConfigContextBuilderResult {
        val profile = TempStore.consume(profileId)
            ?: storeRepository.profileRepository.getProfileById(profileId)
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

        val activeEngineSetting = storeRepository.activeEngineSettingFlow.first()

        val allProxiesMap = mapOf(
            profileModel.id to profileModel
        )

        return EngineConfigContextBuilderResult(
            ecContext = EngineConfigContext(
                node = profileModel,
                engineConfig = activeEngineSetting,
                allProxiesMap = allProxiesMap,
                isTunEnabled = activeEngineSetting.inbound.tun.enable,
            ),
            errors = errors,
            warnings = warnings,
        )
    }
}