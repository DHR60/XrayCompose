package com.clearpath.xray_compose.viewmodel.uistate

import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.db.entities.ProfileTestItem

data class ProfileWithTest(
    val profile: ProfileModel,
    val test: ProfileTestItem? = null
)
