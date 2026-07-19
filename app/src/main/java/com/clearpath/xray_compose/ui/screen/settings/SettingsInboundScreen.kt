package com.clearpath.xray_compose.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.ui.components.FormBottomSheetContext
import com.clearpath.xray_compose.ui.components.ReusableFormBottomSheet
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.sharedviewmodel.LocalSharedViewModelStoreOwner
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.viewmodel.SettingsViewModel

@Composable
fun SettingsInboundScreen() {
    val parentViewModel = viewModel<SettingsViewModel>(
        viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
    )

    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current
    val activeSetting by parentViewModel.activeEngineSettingFlow.collectAsState()
    val inbound = activeSetting.inbound

    var activeDialogContext by remember { mutableStateOf<FormBottomSheetContext?>(null) }

    val inboundSocksPortLabel = stringResource(R.string.inbound_socks_port)

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inbound_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.logcat_back)
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
                .padding(innerPadding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.inbound_socks_port)) },
                    supportingContent = {
                        Column {
                            Text(stringResource(R.string.inbound_socks_port_desc))
                            Text(
                                text = inbound.port.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "socks_port",
                            title = inboundSocksPortLabel,
                            initialValue = inbound.port.toString(),
                            onConfirm = { newValue ->
                                newValue.toIntOrNull()?.let { port ->
                                    parentViewModel.updateActiveEngineSetting {
                                        it.copy(
                                            inbound = it.inbound.copy(
                                                port = port
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.inbound_sniffing)) },
                    supportingContent = { Text(stringResource(R.string.inbound_sniffing_desc)) },
                    trailingContent = {
                        Switch(checked = inbound.sniff, onCheckedChange = null)
                    },
                    modifier = Modifier.toggleable(
                        value = inbound.sniff,
                        onValueChange = { newValue ->
                            parentViewModel.updateActiveEngineSetting {
                                it.copy(
                                    inbound = it.inbound.copy(
                                        sniff = newValue
                                    )
                                )
                            }
                        },
                        role = Role.Switch
                    )
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.inbound_sniff_override)) },
                    supportingContent = { Text(stringResource(R.string.inbound_sniff_override_desc)) },
                    trailingContent = {
                        Switch(checked = inbound.sniffOverrideDest, onCheckedChange = null)
                    },
                    modifier = Modifier.toggleable(
                        value = inbound.sniffOverrideDest,
                        onValueChange = { newValue ->
                            parentViewModel.updateActiveEngineSetting {
                                it.copy(
                                    inbound = it.inbound.copy(
                                        sniffOverrideDest = newValue
                                    )
                                )
                            }
                        },
                        role = Role.Switch
                    )
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.inbound_allow_lan)) },
                    supportingContent = { Text(stringResource(R.string.inbound_allow_lan_desc)) },
                    trailingContent = {
                        Switch(checked = inbound.allowLan, onCheckedChange = null)
                    },
                    modifier = Modifier.toggleable(
                        value = inbound.allowLan,
                        onValueChange = { newValue ->
                            parentViewModel.updateActiveEngineSetting {
                                it.copy(
                                    inbound = it.inbound.copy(
                                        allowLan = newValue
                                    )
                                )
                            }
                        },
                        role = Role.Switch
                    )
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.inbound_enable_tun)) },
                    supportingContent = { Text(stringResource(R.string.inbound_enable_tun_desc)) },
                    trailingContent = {
                        Switch(checked = inbound.tun.enable, onCheckedChange = null)
                    },
                    modifier = Modifier.toggleable(
                        value = inbound.tun.enable,
                        onValueChange = { newValue ->
                            parentViewModel.updateActiveEngineSetting {
                                it.copy(
                                    inbound = it.inbound.copy(
                                        tun = it.inbound.tun.copy(
                                            enable = newValue
                                        )
                                    )
                                )
                            }
                        },
                        role = Role.Switch
                    )
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