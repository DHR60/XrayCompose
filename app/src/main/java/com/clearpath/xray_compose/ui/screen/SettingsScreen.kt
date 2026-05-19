package com.clearpath.xray_compose.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.SettingsDns
import com.clearpath.xray_compose.ui.navigation.SettingsInbound
import com.clearpath.xray_compose.ui.navigation.SettingsRouting
import com.clearpath.xray_compose.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen() {
    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current

    val viewModel: SettingsViewModel = viewModel()
    viewModel.activeEngineSettingIdFlow.collectAsState().value
    val engineSettingList = viewModel.engineSettingListFlow.collectAsState().value
    val activeEngineSetting = viewModel.activeEngineSettingFlow.collectAsState().value

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = activeEngineSetting.remark.ifEmpty { "Unnamed" },
                            onValueChange = {
                                viewModel.updateEngineSetting(
                                    activeEngineSetting.copy(remark = it)
                                )
                            },
                            label = { Text("Configuration name") },
                            modifier = Modifier.weight(1f)
                        )

                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_right),
                                    contentDescription = "Switch Configuration",
                                    modifier = Modifier.rotate(90f)
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                engineSettingList.forEach { setting ->
                                    DropdownMenuItem(
                                        text = { Text(setting.remark.ifEmpty { "Unnamed" }) },
                                        onClick = {
                                            viewModel.switchActiveEngineSetting(setting.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                val newSetting = ConfigEngineItem(
                                    remark = "New Configuration"
                                )
                                viewModel.addEngineSetting(newSetting)
                                viewModel.switchActiveEngineSetting(newSetting.id)
                            }
                        ) {
                            Text("Add")
                        }
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
                            Text("Copy")
                        }
                        TextButton(
                            onClick = {
                                viewModel.removeEngineSetting(activeEngineSetting.id)
                            },
                            enabled = engineSettingList.size > 1
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
            ListItem(
                headlineContent = { Text("Inbound") },
                supportingContent = { Text("Description for Inbound") },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = "Navigate to Inbound Settings"
                    )
                },
                modifier = Modifier.clickable {
                    navigator.navigate(SettingsInbound)
                }
            )
            ListItem(
                headlineContent = { Text("DNS") },
                supportingContent = { Text("Description for DNS") },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = "Navigate to DNS Settings"
                    )
                },
                modifier = Modifier.clickable {
                    navigator.navigate(SettingsDns)
                }
            )
            ListItem(
                headlineContent = { Text("Routing") },
                supportingContent = { Text("Description for Routing") },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = "Navigate to Routing Settings"
                    )
                },
                modifier = Modifier.clickable {
                    navigator.navigate(SettingsRouting)
                }
            )
        }
    }
}