package com.clearpath.xray_compose.service.formatter

import com.clearpath.xray_compose.data.ProfileModel

interface IFmt {
    fun parse(str: String): Result<ProfileModel>
    fun toUri(profile: ProfileModel): Result<String>
}