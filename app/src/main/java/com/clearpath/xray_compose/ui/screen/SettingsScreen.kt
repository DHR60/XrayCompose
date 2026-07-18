package com.clearpath.xray_compose.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.Logcat
import com.clearpath.xray_compose.ui.navigation.SettingsDns
import com.clearpath.xray_compose.ui.navigation.SettingsInbound
import com.clearpath.xray_compose.ui.navigation.SettingsPerApp
import com.clearpath.xray_compose.ui.navigation.SettingsRouting
import com.clearpath.xray_compose.ui.navigation.SettingsSub
import com.clearpath.xray_compose.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen() {
    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current

    val viewModel: SettingsViewModel = hiltViewModel()
    val engineSettingList = viewModel.engineSettingListFlow.collectAsState().value
    val activeEngineSetting = viewModel.activeEngineSettingFlow.collectAsState().value

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Configuration Selection Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_engine_config),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = activeEngineSetting.remark.ifEmpty { stringResource(R.string.settings_unnamed) },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.settings_active_config)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                engineSettingList.forEach { setting ->
                                    DropdownMenuItem(
                                        text = { Text(setting.remark.ifEmpty { stringResource(R.string.settings_unnamed) }) },
                                        onClick = {
                                            viewModel.switchActiveEngineSetting(setting.id)
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = activeEngineSetting.remark,
                            onValueChange = {
                                viewModel.updateEngineSetting(
                                    activeEngineSetting.copy(remark = it)
                                )
                            },
                            label = { Text(stringResource(R.string.settings_rename)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.removeEngineSetting(activeEngineSetting.id) },
                                enabled = engineSettingList.size > 1,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(stringResource(R.string.settings_delete), modifier = Modifier.padding(start = 4.dp))
                            }

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(
                                    8.dp,
                                    alignment = Alignment.End
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        val engineSettingToCopy = activeEngineSetting.copy(
                                            id = ConfigEngineItem().id,
                                            remark = activeEngineSetting.remark + " Copy"
                                        )
                                        viewModel.addEngineSetting(engineSettingToCopy)
                                        viewModel.switchActiveEngineSetting(engineSettingToCopy.id)
                                    }
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_edit),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(stringResource(R.string.settings_copy), modifier = Modifier.padding(start = 4.dp))
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val newSetting =
                                            ConfigEngineItem(remark = "New Configuration")
                                        viewModel.addEngineSetting(newSetting)
                                        viewModel.switchActiveEngineSetting(newSetting.id)
                                    }
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(stringResource(R.string.settings_add), modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Sub-module Settings Section
            Column {
                Text(
                    text = stringResource(R.string.settings_module_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_inbound)) },
                    supportingContent = { Text(stringResource(R.string.settings_inbound_desc)) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_login), contentDescription = null)
                    },
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null)
                    },
                    modifier = Modifier.clickable { navigator.navigate(SettingsInbound) }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dns)) },
                    supportingContent = { Text(stringResource(R.string.settings_dns_desc)) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_dns), contentDescription = null)
                    },
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null)
                    },
                    modifier = Modifier.clickable { navigator.navigate(SettingsDns) }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_routing)) },
                    supportingContent = { Text(stringResource(R.string.settings_routing_desc)) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_alt_route), contentDescription = null)
                    },
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null)
                    },
                    modifier = Modifier.clickable { navigator.navigate(SettingsRouting) }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_per_app)) },
                    supportingContent = { Text(stringResource(R.string.settings_per_app_desc)) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_apps), contentDescription = null)
                    },
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        navigator.navigate(SettingsPerApp)
                    }
                )
            }

            HorizontalDivider()

            // App Settings Section
            Column {
                Text(
                    text = stringResource(R.string.settings_app_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_appearance)) },
                    supportingContent = { Text(stringResource(R.string.settings_appearance_desc)) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_palette), contentDescription = null)
                    },
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null)
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            val matrix = ColorMatrix().apply { setToSaturation(0f) }
                            colorFilter = ColorFilter.colorMatrix(matrix)
                            alpha = 0.5f
                        }
                        .clickable {
                            // TODO
                        }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_subscription)) },
                    supportingContent = { Text(stringResource(R.string.settings_subscription_desc)) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_data_table), contentDescription = null)
                    },
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        navigator.navigate(SettingsSub)
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_asset_files)) },
                    supportingContent = { Text(stringResource(R.string.settings_asset_files_desc)) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_files), contentDescription = null)
                    },
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null)
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            val matrix = ColorMatrix().apply { setToSaturation(0f) }
                            colorFilter = ColorFilter.colorMatrix(matrix)
                            alpha = 0.5f
                        }
                        .clickable {
                            // TODO
                        }
                )
            }

            HorizontalDivider()

            // Tools Section
            Column {
                Text(
                    text = stringResource(R.string.settings_tools_diagnostics),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_logcat)) },
                    supportingContent = { Text(stringResource(R.string.settings_logcat_desc)) },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_bug_report), contentDescription = null)
                    },
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null)
                    },
                    modifier = Modifier.clickable { navigator.navigate(Logcat) }
                )
            }
        }
    }
}
