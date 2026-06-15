package com.clearpath.xray_compose.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.SettingsSubEditor
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.viewmodel.SettingsSubViewModel

@Composable
fun SettingsSubScreen() {
    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current

    val viewModel: SettingsSubViewModel = hiltViewModel()

    val subList by viewModel.subListFlow.collectAsState()

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text("Subscription Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = {
                            viewModel.createNewSubItem()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = "Add"
                            )
                        }
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
            items(
                count = subList.size,
                key = { index -> subList[index].config.id }
            ) { index ->
                val uiState = subList[index]
                val subItem = uiState.config
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .height(96.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = subItem.remark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (uiState.count > 0) {
                                    Surface(
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = uiState.count.toString(),
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            ),
                                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        },
                        supportingContent = {
                            Column {
                                if (subItem.url.isNotEmpty()) {
                                    Text(
                                        text = "URL: ${subItem.url}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (subItem.lastUpdate > 1767196800000) {
                                    val lastUpdateStr = java.text.SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        java.util.Locale.US
                                    ).format(java.util.Date(subItem.lastUpdate))
                                    Text("Last Updated: $lastUpdateStr")
                                }
                            }
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    IconButton(
                                        onClick = {
                                            navigator.navigate(
                                                SettingsSubEditor(subItem.id)
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
                                            viewModel.removeSubItem(subItem.id)
                                        },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = "Delete Rule"
                                        )
                                    }
                                }
                                if (subItem.url.isNotEmpty()) {
                                    if (uiState.isUpdating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        TextButton(
                                            onClick = {
                                                viewModel.updateSubForNetwork(subItem.id)
                                            },
                                            modifier = Modifier.height(36.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                                horizontal = 12.dp,
                                                vertical = 0.dp
                                            )
                                        ) {
                                            Text(
                                                "Update",
                                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                                            )
                                        }
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