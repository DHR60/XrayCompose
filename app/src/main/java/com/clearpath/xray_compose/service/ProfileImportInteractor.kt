package com.clearpath.xray_compose.service

import android.content.Context
import com.clearpath.xray_compose.core.NetworkManager
import com.clearpath.xray_compose.data.importer.ProfileImporter
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.utils.Utils
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.headers
import javax.inject.Inject

class ProfileImportInteractor @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val configRepo: ConfigRepository,
    private val prefsRepo: PreferencesRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend fun importFromClipboard(targetSubId: String = "") {
        val clipboardText = Utils.getClipboard(context)
        val importer = ProfileImporter(
            profileRepo = profileRepo,
            prefsRepo = prefsRepo,
            isSub = false,
            targetSubId = targetSubId,
        )
        importer.addBatchServers(clipboardText)
    }

    suspend fun updateSub(targetSubId: String) {
        val subItem =
            configRepo.getConfig().subList.firstOrNull { it.id == targetSubId } ?: run {
                error("Sub with id $targetSubId not found")
            }
        if (subItem.url.isEmpty()) {
            error("Not a remote sub")
        }
        val regex = if (subItem.filter.isNotEmpty()) {
            try {
                Regex(subItem.filter)
            } catch (e: Exception) {
                error("Invalid regex filter: ${e.message}")
            }
        } else null
        NetworkManager.safeRequestAuto {
            get(subItem.url) {
                headers {
                    if (subItem.userAgent.isNotEmpty()) {
                        append("User-Agent", subItem.userAgent)
                    } else {
                        append("User-Agent", "v2rayNG/2.2.3")
                    }
                }
            }.bodyAsText()
        }.onSuccess {
            val importer = ProfileImporter(
                profileRepo = profileRepo,
                prefsRepo = prefsRepo,
                isSub = true,
                targetSubId = targetSubId,
            )
            if (regex != null) {
                importer.profileFilter = { model ->
                    regex.matches(model.remark)
                }
            }
            importer.addBatchServers(it)
            configRepo.updateSubItem(
                subItem.copy(
                    lastUpdate = System.currentTimeMillis()
                )
            )
        }.onFailure {
            error("Failed to update sub: ${it.message}")
        }
    }
}
