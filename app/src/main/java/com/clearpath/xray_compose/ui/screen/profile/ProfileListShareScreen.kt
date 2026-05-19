package com.clearpath.xray_compose.ui.screen.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.clearpath.xray_compose.viewmodel.ProfileListShareViewModel

@Composable
fun ProfileListShareScreen(id: String) {
    // val parentViewModel = viewModel<ProfileListViewModel>(
    //     viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
    // )
    // val activeProfileId = parentViewModel.activeProfileIdFlow.collectAsState().value
    // ProfileListShareScreenContent(id)

    val viewModel = viewModel<ProfileListShareViewModel>(
        factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                ProfileListShareViewModel(
                    id = id,
                    application = application
                )
            }
        }
    )

    val display = viewModel.displayFlow.collectAsState().value
    Text(text = display)
}

// @Composable
// fun ProfileListShareScreenContent(id: String) {
//     Text(
//         text = "Share Profile List: $id, activeProfileId: $activeProfileId"
//     )
// }