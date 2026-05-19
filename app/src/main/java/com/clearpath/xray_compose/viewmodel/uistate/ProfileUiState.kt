package com.clearpath.xray_compose.viewmodel.uistate

import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.db.entities.ProfileTestItem

data class ProfileUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,

    val profileModel: ProfileModel = ProfileModel(),
    val profileTestItem: ProfileTestItem? = null,
) {
    companion object {
        val Empty = { ProfileUiState() }
        fun fromProfileModel(
            item: ProfileModel,
            testItem: ProfileTestItem? = null
        ): ProfileUiState {
            return ProfileUiState(
                profileModel = item,
                profileTestItem = testItem,
            )
        }
    }

    val id get() = profileModel.id

    fun toProfileModel(): ProfileModel {
        return profileModel
    }
}
