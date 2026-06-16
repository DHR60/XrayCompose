package com.clearpath.xray_compose.data.importer

import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.service.formatter.FmtFact
import com.clearpath.xray_compose.utils.Base64Util
import com.clearpath.xray_compose.utils.LogUtil

class ProfileImporter(
    private val profileRepo: ProfileRepository,
    private val prefsRepo: PreferencesRepository,
    private val isSub: Boolean,
    private val targetSubId: String = "",
) {
    var profileFilter: (ProfileModel) -> Boolean = { true }

    suspend fun addBatchServers(data: String): Int {
        // TODO: inner url, shadowsocks SIP008, wireguard config, custom config...
        val subId = targetSubId.ifEmpty { prefsRepo.activeSubIdFlow.value } ?: ""

        val profiles = doParse(data)
            .filter { profileFilter(it) }
            .map {
                it.copy(
                    isSub = isSub,
                    subId = subId,
                )
            }

        if (profiles.isNotEmpty() && subId.isNotEmpty() && isSub) {
            profileRepo.deleteSubProfiles(subId)
        }

        if (profiles.isNotEmpty()) {
            profileRepo.insertProfiles(profiles)
        }
        return profiles.size
    }

    private fun doParse(data: String): List<ProfileModel> {
        val orderedList = mutableListOf<Pair<Int, ProfileModel>>()
        val unorderedList = mutableListOf<ProfileModel>()

        var count = 0
        if (Base64Util.isBase64String(data, true)) {
            val list = parseCommonShareUrl(Base64Util.decodeBase64(data))
            orderedList.addAll(list)
            count += list.size
        }
        if (count <= 0)
        {
            val list = parseCommonShareUrl(data)
            orderedList.addAll(list)
            count += list.size
        }
        if (count <= 0) {
            val decodedBytes = Base64Util.decodeBase64WithMaxTolerance(data)
            if (decodedBytes.isNotEmpty()) {
                val decodedString = String(decodedBytes, Charsets.UTF_8)
                val list = parseCommonShareUrl(decodedString)
                orderedList.addAll(list)
                count += list.size
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