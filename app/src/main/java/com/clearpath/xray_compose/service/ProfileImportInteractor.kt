package com.clearpath.xray_compose.service

import android.content.Context
import com.clearpath.xray_compose.core.NetworkManager
import com.clearpath.xray_compose.data.StoreRepos
import com.clearpath.xray_compose.data.importer.ProfileImporter
import com.clearpath.xray_compose.utils.Utils
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.headers

class ProfileImportInteractor(
    private val storeRepos: StoreRepos,
    private val targetSubId: String = "",
    private val context: Context
) {
    suspend fun importFromClipboard() {
        val clipboardText = Utils.getClipboard(context)
        val importer = ProfileImporter(
            storeRepos = storeRepos,
            isSub = false,
            targetSubId = targetSubId,
        )
        importer.addBatchServers(clipboardText)
    }

    suspend fun updateSub() {
        val subItem =
            storeRepos.configRepo.subListFlow.value.firstOrNull { it.id == targetSubId } ?: run {
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
                storeRepos = storeRepos,
                isSub = true,
                targetSubId = targetSubId,
            )
            if (regex != null) {
                importer.profileFilter = { model ->
                    regex.matches(model.remark)
                }
            }
            importer.addBatchServers(it)
            storeRepos.configRepo.updateSubItem(
                subItem.copy(
                    lastUpdate = System.currentTimeMillis()
                )
            )
        }.onFailure {
            error("Failed to update sub: ${it.message}")
        }
    }
}