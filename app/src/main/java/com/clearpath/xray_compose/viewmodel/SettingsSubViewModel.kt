package com.clearpath.xray_compose.viewmodel

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigSubItem
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.service.ProfileImportInteractor
import com.clearpath.xray_compose.utils.LogUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SettingsSubViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val configRepository: ConfigRepository,
    private val profileImportInteractor: ProfileImportInteractor
) : ViewModel() {

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
                profileImportInteractor.updateSub(subId)
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

    private var reorderJob: Job? = null

    fun reorderSubItems(from: LazyListItemInfo, to: LazyListItemInfo) {
        val currentUiList = _subListFlow.value
        val fromIndex = from.index
        val toIndex = to.index

        val fromItem = currentUiList.getOrNull(fromIndex)
        val toItem = currentUiList.getOrNull(toIndex)

        if (fromItem?.config?.id != from.key || toItem?.config?.id != to.key) {
            LogUtil.e("SettingsSubViewModel Failed to reorder sub items: Index mismatch")
            return
        }

        val updatedUiList = currentUiList.toMutableList()
        updatedUiList.removeAt(fromIndex)
        updatedUiList.add(toIndex, fromItem)
        _subListFlow.value = updatedUiList

        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(500.milliseconds)
            val finalList = _subListFlow.value.map { it.config }
            withContext(Dispatchers.IO) {
                try {
                    configRepository.updateSubList {
                        finalList
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    LogUtil.e("SettingsSubViewModel Error updating sub list", e)
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
