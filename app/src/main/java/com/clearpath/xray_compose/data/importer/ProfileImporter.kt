package com.clearpath.xray_compose.data.importer

import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.StoreRepos
import com.clearpath.xray_compose.service.formatter.FmtFact
import com.clearpath.xray_compose.utils.LogUtil

class ProfileImporter(
    private val storeRepos: StoreRepos,
    private val isSub: Boolean,
    private val targetSubId: String = "",
) {
    suspend fun addBatchServers(data: String): Int {
        // TODO: inner url, shadowsocks SIP008, wireguard config, custom config...
        val subId = targetSubId.ifEmpty { storeRepos.prefsRepo.activeSubIdFlow.value } ?: ""

        if (subId.isNotEmpty() && isSub) {
            storeRepos.profileRepo.deleteSubProfiles(subId)
        }

        val profiles = parseCommonShareUrl(data)
            .map { it.second }
            .map {
                it.copy(
                    isSub = isSub,
                    subId = subId,
                )
            }
            .toList()

        if (profiles.isNotEmpty()) {
            storeRepos.profileRepo.insertProfiles(profiles)
        }
        return profiles.size
    }

    fun parseCommonShareUrl(data: String): List<Pair<Int, ProfileModel>> {
        val lineCount = data.count { it == '\n' } + 1
        val list = ArrayList<Pair<Int, ProfileModel>>(lineCount)
        data.lineSequence()
            .forEachIndexed { index, string ->
                if (string.isBlank()) {
                    return@forEachIndexed
                }
                val line = string.trim()
                FmtFact.parseUrl(line).onSuccess { profile ->
                    list.add(index to profile)
                }.onFailure {
                    LogUtil.e("Failed to parse line $index: ${it.message}", it)
                }
            }
        return list
    }
}