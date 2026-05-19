package com.clearpath.xray_compose.ui.screen.profile

import androidx.compose.runtime.Composable

@Composable
fun ProfileCreateScreen(
    templateId: String,
) {
    ProfileEditorScreen(
        id = templateId,
        isNew = true,
    )
}