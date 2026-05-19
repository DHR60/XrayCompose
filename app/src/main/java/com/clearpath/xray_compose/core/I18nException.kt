package com.clearpath.xray_compose.core

open class I18nException(
    stringResId: Int,
    vararg val args: Any
) : Exception("I18n Error Code: $stringResId")