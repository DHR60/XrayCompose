package com.clearpath.xray_compose.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.staticCompositionLocalOf

val LocalRootInnerPadding = staticCompositionLocalOf<PaddingValues> {
    error("RootInnerPadding not provided")
}