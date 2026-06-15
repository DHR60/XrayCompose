package com.clearpath.xray_compose.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.ui.components.EditableTrailingIconField
import com.clearpath.xray_compose.ui.components.FormBottomSheetContext
import com.clearpath.xray_compose.ui.components.ReusableFormBottomSheet
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.sharedviewmodel.LocalSharedViewModelStoreOwner
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.viewmodel.SettingsSubEditorViewModel
import com.clearpath.xray_compose.viewmodel.SettingsSubViewModel

@Composable
fun SettingsSubEditorScreen(
    id: String,
) {
    val parentViewModel = hiltViewModel<SettingsSubViewModel>(
        viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
    )
    val viewModel = hiltViewModel<SettingsSubEditorViewModel, SettingsSubEditorViewModel.Factory>(
        creationCallback = { factory -> factory.create(id, parentViewModel) }
    )

    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current
    val subItem by viewModel.subItemFlow.collectAsState()

    var activeDialogContext by remember { mutableStateOf<FormBottomSheetContext?>(null) }

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text("Subscription Editor") },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                EditableTrailingIconField(
                    value = subItem.remark,
                    onValueChange = { newRemark ->
                        viewModel.updateSubItem { currentSubItem ->
                            currentSubItem.copy(remark = newRemark)
                        }
                    },
                    label = { Text("Remark") },
                    modifier = Modifier.fillMaxWidth(),
                    onEditIconClick = {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "remark",
                            title = "Edit Remark",
                            initialValue = subItem.remark,
                            onConfirm = { newRemark ->
                                viewModel.updateSubItem { currentSubItem ->
                                    currentSubItem.copy(remark = newRemark)
                                }
                            }
                        )
                    }
                )
            }
            item {
                EditableTrailingIconField(
                    value = subItem.url,
                    onValueChange = { newUrl ->
                        viewModel.updateSubItem { currentSubItem ->
                            currentSubItem.copy(url = newUrl)
                        }
                    },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    onEditIconClick = {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "url",
                            title = "Edit URL",
                            initialValue = subItem.url,
                            onConfirm = { newUrl ->
                                viewModel.updateSubItem { currentSubItem ->
                                    currentSubItem.copy(url = newUrl)
                                }
                            }
                        )
                    }
                )
            }
            item {
                EditableTrailingIconField(
                    value = subItem.userAgent,
                    onValueChange = { newUserAgent ->
                        viewModel.updateSubItem { currentSubItem ->
                            currentSubItem.copy(userAgent = newUserAgent)
                        }
                    },
                    label = { Text("User Agent") },
                    modifier = Modifier.fillMaxWidth(),
                    onEditIconClick = {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "userAgent",
                            title = "Edit User Agent",
                            initialValue = subItem.userAgent,
                            onConfirm = { newUserAgent ->
                                viewModel.updateSubItem { currentSubItem ->
                                    currentSubItem.copy(userAgent = newUserAgent)
                                }
                            }
                        )
                    }
                )
            }
            item {
                EditableTrailingIconField(
                    value = subItem.filter,
                    onValueChange = { newFilter ->
                        viewModel.updateSubItem { currentSubItem ->
                            currentSubItem.copy(filter = newFilter)
                        }
                    },
                    label = { Text("Filter") },
                    modifier = Modifier.fillMaxWidth(),
                    onEditIconClick = {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "filter",
                            title = "Edit Filter",
                            initialValue = subItem.filter,
                            onConfirm = { newFilter ->
                                viewModel.updateSubItem { currentSubItem ->
                                    currentSubItem.copy(filter = newFilter)
                                }
                            }
                        )
                    }
                )
            }
        }
    }
    ReusableFormBottomSheet(
        context = activeDialogContext,
        onDismiss = {
            activeDialogContext = null
        }
    )
}