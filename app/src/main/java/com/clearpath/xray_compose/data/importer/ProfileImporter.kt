package com.clearpath.xray_compose.data.importer

import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.StoreRepos
import com.clearpath.xray_compose.service.formatter.FmtFact
import com.clearpath.xray_compose.utils.Base64Util
import com.clearpath.xray_compose.utils.LogUtil

class ProfileImporter(
    private val storeRepos: StoreRepos,
    private val isSub: Boolean,
    private val targetSubId: String = "",
) {
    var profileFilter: (ProfileModel) -> Boolean = { true }

    suspend fun addBatchServers(data: String): Int {
        // TODO: inner url, shadowsocks SIP008, wireguard config, custom config...
        val subId = targetSubId.ifEmpty { storeRepos.prefsRepo.activeSubIdFlow.value } ?: ""

        val profiles = doParse(data)
            .filter { profileFilter(it) }
            .map {
                it.copy(
                    isSub = isSub,
                    subId = subId,
                )
            }

        if (profiles.isNotEmpty() && subId.isNotEmpty() && isSub) {
            storeRepos.profileRepo.deleteSubProfiles(subId)
        }

        if (profiles.isNotEmpty()) {
            storeRepos.profileRepo.insertProfiles(profiles)
        }
        return profiles.size
    }

    private fun doParse(data: String): List<ProfileModel> {
        val orderedList = mutableListOf<Pair<Int, ProfileModel>>()
        val unorderedList = mutableListOf<ProfileModel>()
        if (Base64Util.isBase64String(data, true)) {
            orderedList.addAll(parseCommonShareUrl(Base64Util.decodeBase64(data)))
        } else {
            val list = parseCommonShareUrl(data)
            orderedList.addAll(list)
            if (list.isEmpty()) {
                val decodedBytes = Base64Util.decodeBase64WithMaxTolerance(data)
                if (decodedBytes.isNotEmpty()) {
                    val decodedString = String(decodedBytes, Charsets.UTF_8)
                    unorderedList.addAll(parseCommonShareUrl(decodedString).map { it.second })
                }
            }
        }
        // order by line number, then append unordered list
        val result = orderedList.sortedBy { it.first }.map { it.second }.toMutableList()
        result.addAll(unorderedList)
        return result.distinct()
    }

    private fun parseCommonShareUrl(data: String): List<Pair<Int, ProfileModel>> {
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