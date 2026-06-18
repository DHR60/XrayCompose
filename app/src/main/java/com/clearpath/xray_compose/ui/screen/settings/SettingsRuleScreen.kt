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
import com.clearpath.xray_compose.ui.components.StringListEditor
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.sharedviewmodel.LocalSharedViewModelStoreOwner
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.viewmodel.SettingsRoutingViewModel
import com.clearpath.xray_compose.viewmodel.SettingsRuleViewModel

@Composable
fun SettingsRuleScreen(
    id: String,
) {
    val parentViewModel = hiltViewModel<SettingsRoutingViewModel>(
        viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
    )
    val viewModel = hiltViewModel<SettingsRuleViewModel, SettingsRuleViewModel.Factory>(
        creationCallback = { factory -> factory.create(id, parentViewModel) }
    )

    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current
    val rule by viewModel.ruleFlow.collectAsState()

    var activeDialogContext by remember { mutableStateOf<FormBottomSheetContext?>(null) }

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text("Rule Settings") },
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
                    value = rule.remark,
                    onValueChange = { newRemark ->
                        viewModel.updateRule { currentRule ->
                            currentRule.copy(remark = newRemark)
                        }
                    },
                    label = { Text("Remark") },
                    modifier = Modifier.fillMaxWidth(),
                    onEditIconClick = {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "remark",
                            title = "Edit Remark",
                            initialValue = rule.remark,
                            onConfirm = { newRemark ->
                                viewModel.updateRule { currentRule ->
                                    currentRule.copy(remark = newRemark)
                                }
                            }
                        )
                    }
                )
            }
            item {
                EditableTrailingIconField(
                    value = rule.outboundTag,
                    onValueChange = { newOutboundTag ->
                        viewModel.updateRule { currentRule ->
                            currentRule.copy(outboundTag = newOutboundTag)
                        }
                    },
                    label = { Text("OutboundTag") },
                    modifier = Modifier.fillMaxWidth(),
                    onEditIconClick = {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "outboundTag",
                            title = "Edit OutboundTag",
                            initialValue = rule.outboundTag,
                            onConfirm = { newOutboundTag ->
                                viewModel.updateRule { currentRule ->
                                    currentRule.copy(outboundTag = newOutboundTag)
                                }
                            }
                        )
                    }
                )
            }
            item {
                StringListEditor(
                    label = "Domain",
                    items = rule.domain,
                    onItemsChange = { newDomainList ->
                        viewModel.updateRule { currentRule ->
                            currentRule.copy(domain = newDomainList)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                StringListEditor(
                    label = "IP",
                    items = rule.ip,
                    onItemsChange = { newIPList ->
                        viewModel.updateRule { currentRule ->
                            currentRule.copy(ip = newIPList)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
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