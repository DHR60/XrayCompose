package com.clearpath.xray_compose.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.SettingsSubEditor
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.viewmodel.SettingsSubViewModel

@Composable
fun SettingsSubScreen() {
    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current

    val viewModel: SettingsSubViewModel = viewModel()

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
                key = { index -> subList[index].id }
            ) { index ->
                val subItem = subList[index]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(subItem.remark) },
                        supportingContent = {
                            Column {
                                if (subItem.url.isNotEmpty()) {
                                    Text("URL: ${subItem.url}")
                                }
                                if (subItem.lastUpdateTime.isNotEmpty()) {
                                    Text("Last Updated: ${subItem.lastUpdateTime}")
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
                                    TextButton(
                                        onClick = {
                                            // TODO
                                        }
                                    ) {
                                        Text("Update")
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