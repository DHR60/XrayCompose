package com.clearpath.xray_compose.ui.screen.profile

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.viewmodel.ProfileListShareViewModel

@Composable
fun ProfileListShareScreen(id: String) {
    val viewModel = hiltViewModel<ProfileListShareViewModel, ProfileListShareViewModel.Factory>(
        creationCallback = { factory -> factory.create(id) }
    )
    val scrollState = rememberScrollState()

    val display = viewModel.displayFlow.collectAsState().value
    Text(
        text = display, modifier = Modifier
            .verticalScroll(scrollState)
    )
}

// @Composable
// fun ProfileListShareScreenContent(id: String) {
//     Text(
//         text = "Share Profile List: $id, activeProfileId: $activeProfileId"
//     )
// }