package com.clearpath.xray_compose.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.SettingsRule
import com.clearpath.xray_compose.ui.navigation.sharedviewmodel.LocalSharedViewModelStoreOwner
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.viewmodel.SettingsRoutingViewModel
import com.clearpath.xray_compose.viewmodel.SettingsViewModel

@Composable
fun SettingsRoutingScreen() {
    val parentViewModel = viewModel<SettingsViewModel>(
        viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
    )
    val viewModel: SettingsRoutingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                SettingsRoutingViewModel(
                    settingsViewModel = parentViewModel,
                    application = application
                )
            }
        }
    )

    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current
    val domainStrategy by viewModel.domainStrategyFlow.collectAsState()
    val enableAutoDetachment by viewModel.enableAutoDetachmentFlow.collectAsState()
    val ruleList by viewModel.ruleListFlow.collectAsState()

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text("Routing Settings") },
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
                .padding(innerPadding)
        ) {
            item {
                Text(
                    text = "Routing settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = domainStrategy.ifEmpty { GlobalConst.AsIs },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transport Security") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = {
                            TrailingIcon(
                                expanded = expanded
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            unfocusedTextColor = if (domainStrategy.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        GlobalConst.domainStrategyList.forEach { domainStrategy ->
                            DropdownMenuItem(
                                text = { Text(domainStrategy) },
                                onClick = {
                                    viewModel.updateDomainStrategy(domainStrategy)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("Auto Detachment") },
                    supportingContent = { Text("Automatically separate domain name rules and IP rules into OR logic") },
                    trailingContent = {
                        Switch(checked = enableAutoDetachment, onCheckedChange = null)
                    },
                    modifier = Modifier.toggleable(
                        value = enableAutoDetachment,
                        onValueChange = { newValue ->
                            viewModel.updateEnableAutoDetachment(newValue)
                        },
                        role = Role.Switch
                    )
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rules",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            viewModel.createNewRule()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Add Rule"
                        )
                    }
                }
            }
            items(
                count = ruleList.size,
                key = { index -> ruleList[index].id }
            ) { index ->
                val rule = ruleList[index]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(rule.remark) },
                        supportingContent = {
                            Column {
                                Text("Type: ${rule.ruleType}")
                                Text("Outbound Tag: ${rule.outboundTag}")
                                if (rule.domain.isNotBlank()) {
                                    Text("Domain: ${rule.domain}")
                                } else if (rule.ip.isNotBlank()) {
                                    Text("IP: ${rule.ip}")
                                }
                            }
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Switch(
                                    checked = rule.enable,
                                    onCheckedChange = { newValue ->
                                        viewModel.setRuleEnabled(rule.id, newValue)
                                    }
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            navigator.navigate(
                                                SettingsRule(rule.id)
                                            )
                                        },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_edit),
                                            contentDescription = "Edit Rule"
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.removeRule(rule.id)
                                        },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = "Delete Rule"
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}