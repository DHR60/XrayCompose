package com.clearpath.xray_compose.viewmodel.uistate

import com.clearpath.xray_compose.data.ProfileModel

data class ProfileUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,

    val profileModel: ProfileModel = ProfileModel(),
) {
    companion object {
        val Empty = { ProfileUiState() }
        fun fromProfileModel(item: ProfileModel): ProfileUiState {
            return ProfileUiState(
                profileModel = item,
            )
        }
    }

    val id get() = profileModel.id

    fun toProfileModel(): ProfileModel {
        return profileModel
    }
}
