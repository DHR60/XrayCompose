package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigSubItem
import com.clearpath.xray_compose.data.StoreRepos
import com.clearpath.xray_compose.data.repo.configRepository
import com.clearpath.xray_compose.data.repo.profileRepository
import com.clearpath.xray_compose.service.ProfileImportInteractor
import com.clearpath.xray_compose.utils.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsSubViewModel(application: Application) : AndroidViewModel(application) {
    private val profileRepository = application.profileRepository
    private val configRepository = application.configRepository

    private val _subListFlow = MutableStateFlow<List<SubItemUiState>>(emptyList())
    val subListFlow = _subListFlow.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.subListFlow.collect { subList ->
                val currentMap = _subListFlow.value.associateBy { it.config.id }
                _subListFlow.value = subList.map { config ->
                    val oldState = currentMap[config.id]
                    val count = profileRepository.getProfileCountForSub(config.id)
                    SubItemUiState(
                        config = config,
                        count = count,
                        isUpdating = oldState?.isUpdating ?: false
                    )
                }
            }
        }
    }

    fun updateSubList(subList: List<ConfigSubItem>) {
        viewModelScope.launch {
            configRepository.updateSubList {
                subList
            }
        }
    }

    fun createNewSubItem() {
        addSubItem(
            ConfigSubItem(
                remark = "New Sub Item",
            )
        )
    }

    fun addSubItem(subItem: ConfigSubItem) {
        val newList = _subListFlow.value.map { it.config } + listOf(subItem)
        updateSubList(newList)
    }

    fun updateSubItem(subItem: ConfigSubItem) {
        val newList = _subListFlow.value.map {
            if (it.config.id == subItem.id) {
                subItem
            } else {
                it.config
            }
        }
        updateSubList(newList)
    }

    fun removeSubItem(subId: String) {
        val newList = _subListFlow.value.map { it.config }.filter { it.id != subId }
        updateSubList(newList)
        viewModelScope.launch(Dispatchers.IO) {
            profileRepository.deleteProfilesBySubid(subId)
        }
    }

    fun updateSubForNetwork(subId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _subListFlow.value = _subListFlow.value.map {
                    if (it.config.id == subId) it.copy(isUpdating = true) else it
                }
                val profileImportInteractor = ProfileImportInteractor(
                    storeRepos = StoreRepos.getOrBuildSingleton(application),
                    targetSubId = subId,
                    context = application
                )
                profileImportInteractor.updateSub()
                val count = profileRepository.getProfileCountForSub(subId)
                _subListFlow.value = _subListFlow.value.map {
                    if (it.config.id == subId) it.copy(isUpdating = false, count = count) else it
                }
            } catch (e: Exception) {
                LogUtil.e("SettingsSubViewModel Error updating sub for network", e)
                _subListFlow.value = _subListFlow.value.map {
                    if (it.config.id == subId) it.copy(isUpdating = false) else it
                }
            }
        }
    }
}

data class SubItemUiState(
    val config: ConfigSubItem,
    val count: Int = 0,
    val isUpdating: Boolean = false
)
