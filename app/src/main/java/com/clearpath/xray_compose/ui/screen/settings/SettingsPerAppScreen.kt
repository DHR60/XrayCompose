package com.clearpath.xray_compose.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.ui.components.AppListItem
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.viewmodel.SettingsPerAppViewModel

@Composable
fun SettingsPerAppScreen() {
    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current

    val viewModel: SettingsPerAppViewModel = hiltViewModel()

    val isBusy by viewModel.isBusyFlow.collectAsState()
    val errorMessage by viewModel.errorMessageFlow.collectAsState()
    val perAppEnabled by viewModel.perAppEnabledFlow.collectAsState()
    val bypassPerApp by viewModel.perAppBypassFlow.collectAsState()
    val displayAppPackages by viewModel.displayAppPackagesFlow.collectAsState()
    val selectedAppPackages by viewModel.selectedAppPackagesFlow.collectAsState()
    val searchQuery by viewModel.searchQueryFlow.collectAsState()

    val options = listOf("Disabled", "Proxy", "Bypass")

    val searchMode = remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = {
                    if (searchMode.value) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search apps...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Per-app Settings")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Row {
                        if (!searchMode.value) {
                            IconButton(onClick = { searchMode.value = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = "Search"
                                )
                            }
                            var moreExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = {
                                        moreExpanded = true
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_more_vert),
                                        contentDescription = "More Actions"
                                    )
                                }
                                DropdownMenu(
                                    expanded = moreExpanded,
                                    onDismissRequest = { moreExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        onClick = {
                                            moreExpanded = false
                                            viewModel.refreshAllAppPackages()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Import from Clipboard") },
                                        onClick = {
                                            moreExpanded = false
                                            viewModel.importFromClipboard()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export to Clipboard") },
                                        onClick = {
                                            moreExpanded = false
                                            viewModel.exportToClipboard()
                                        }
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { searchMode.value = false }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Close Search"
                                )
                            }
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isBusy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            } else {
                // padding
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (errorMessage != null) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Clear Error"
                            )
                        }
                    }
                }
            }
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        selected = when (option) {
                            "Disabled" -> !perAppEnabled
                            "Proxy" -> perAppEnabled && !bypassPerApp
                            "Bypass" -> perAppEnabled && bypassPerApp
                            else -> false
                        },
                        onClick = {
                            when (option) {
                                "Disabled" -> viewModel.updatePerAppEnabled(false)
                                "Proxy" -> viewModel.updatePerAppMode(
                                    enabled = true,
                                    bypass = false
                                )

                                "Bypass" -> viewModel.updatePerAppMode(
                                    enabled = true,
                                    bypass = true
                                )
                            }
                        }
                    ) {
                        Text(option)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayAppPackages, key = { it.packageName }) { item ->
                    AppListItem(
                        appItemInfo = item,
                        isSelected = selectedAppPackages.contains(item.packageName),
                        iconCacheMap = viewModel.iconCache,
                        fetchAppIcon = viewModel::fetchAppIcon,
                        onToggleSelection = {
                            viewModel.toggleAppPackageSelection(item.packageName)
                        }
                    )
                }
            }
        }
    }
}
