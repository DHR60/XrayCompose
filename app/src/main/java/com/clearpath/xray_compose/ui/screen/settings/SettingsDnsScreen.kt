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
fun SettingsDnsScreen() {
    val parentViewModel = viewModel<SettingsViewModel>(
        viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
    )

    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current
    val activeSetting by parentViewModel.activeEngineSettingFlow.collectAsState()
    val dns = activeSetting.dns

    var activeDialogContext by remember { mutableStateOf<FormBottomSheetContext?>(null) }

    val dnsRemoteDnsLabel = stringResource(R.string.dns_remote_dns)
    val dnsLocalDnsLabel = stringResource(R.string.dns_local_dns)
    val dnsAdditionalHostsLabel = stringResource(R.string.dns_additional_hosts)

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dns_settings_title)) },
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
                    headlineContent = { Text(stringResource(R.string.dns_remote_dns)) },
                    supportingContent = {
                        Column {
                            Text(stringResource(R.string.dns_remote_dns_desc))
                            Text(
                                text = dns.remoteDns.ifBlank { stringResource(R.string.dns_not_set) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "remote_dns",
                            title = dnsRemoteDnsLabel,
                            initialValue = dns.remoteDns,
                            onConfirm = { newValue ->
                                parentViewModel.updateActiveEngineSetting {
                                    it.copy(
                                        dns = it.dns.copy(
                                            remoteDns = newValue
                                        )
                                    )
                                }
                            }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.dns_local_dns)) },
                    supportingContent = {
                        Column {
                            Text(stringResource(R.string.dns_local_dns_desc))
                            Text(
                                text = dns.localDns.ifBlank { stringResource(R.string.dns_not_set) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "local_dns",
                            title = dnsLocalDnsLabel,
                            initialValue = dns.localDns,
                            onConfirm = { newValue ->
                                parentViewModel.updateActiveEngineSetting {
                                    it.copy(
                                        dns = it.dns.copy(
                                            localDns = newValue
                                        )
                                    )
                                }
                            }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.dns_enable_fake_dns)) },
                    supportingContent = { Text(stringResource(R.string.dns_enable_fake_dns_desc)) },
                    trailingContent = {
                        Switch(checked = dns.enableFakeDns, onCheckedChange = null)
                    },
                    modifier = Modifier.toggleable(
                        value = dns.enableFakeDns,
                        onValueChange = { newValue ->
                            parentViewModel.updateActiveEngineSetting {
                                it.copy(
                                    dns = it.dns.copy(
                                        enableFakeDns = newValue
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
                    headlineContent = { Text(stringResource(R.string.dns_serve_stale)) },
                    supportingContent = { Text(stringResource(R.string.dns_serve_stale_desc)) },
                    trailingContent = {
                        Switch(checked = dns.serveStale, onCheckedChange = null)
                    },
                    modifier = Modifier.toggleable(
                        value = dns.serveStale,
                        onValueChange = { newValue ->
                            parentViewModel.updateActiveEngineSetting {
                                it.copy(
                                    dns = it.dns.copy(
                                        serveStale = newValue
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
                    headlineContent = { Text(stringResource(R.string.dns_parallel_query)) },
                    supportingContent = { Text(stringResource(R.string.dns_parallel_query_desc)) },
                    trailingContent = {
                        Switch(checked = dns.parallelQuery, onCheckedChange = null)
                    },
                    modifier = Modifier.toggleable(
                        value = dns.parallelQuery,
                        onValueChange = { newValue ->
                            parentViewModel.updateActiveEngineSetting {
                                it.copy(
                                    dns = it.dns.copy(
                                        parallelQuery = newValue
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
                    headlineContent = { Text(stringResource(R.string.dns_additional_hosts)) },
                    supportingContent = {
                        Column {
                            Text(stringResource(R.string.dns_additional_hosts_desc))
                            Text(
                                text = if (dns.additionalHosts.isBlank()) stringResource(R.string.dns_not_set) else stringResource(
                                    R.string.dns_entries_count,
                                    dns.additionalHosts.lines().size
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        activeDialogContext = FormBottomSheetContext(
                            fieldKey = "additional_hosts",
                            title = dnsAdditionalHostsLabel,
                            initialValue = dns.additionalHosts,
                            onConfirm = { newValue ->
                                parentViewModel.updateActiveEngineSetting {
                                    it.copy(
                                        dns = it.dns.copy(
                                            additionalHosts = newValue
                                        )
                                    )
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