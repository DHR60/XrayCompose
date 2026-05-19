package com.clearpath.xray_compose.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.tempstore.TempStore
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.ProfileCreate
import com.clearpath.xray_compose.ui.navigation.ProfileEdit
import com.clearpath.xray_compose.ui.navigation.ProfileListShare
import com.clearpath.xray_compose.viewmodel.ProfileListViewModel
import com.clearpath.xray_compose.viewmodel.uistate.ProfileUiState

@Composable
fun ProfileListScreen() {
    val viewModel: ProfileListViewModel = viewModel()
    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current
    val subItems by viewModel.subItemsFlow.collectAsState()
    val activeSubProfiles by viewModel.activeSubProfilesFlow.collectAsState()
    val activeSubId by viewModel.activeSubIdFlow.collectAsState()
    val activeProfileId by viewModel.activeProfileIdFlow.collectAsState()

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text("Profile List") },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        var addProfileMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = {
                                    addProfileMenuExpanded = true
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = "Add Profile"
                                )
                            }
                            DropdownMenu(
                                expanded = addProfileMenuExpanded,
                                onDismissRequest = { addProfileMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Import from Clipboard") },
                                    onClick = {
                                        addProfileMenuExpanded = false
                                        viewModel.importFromClipboard()
                                    }
                                )
                                var manualAddSubExpanded by remember { mutableStateOf(false) }
                                Box {
                                    DropdownMenuItem(
                                        text = { Text("Add Manual") },
                                        trailingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_arrow_right),
                                                contentDescription = "Add Manual Submenu"
                                            )
                                        },
                                        onClick = {
                                            manualAddSubExpanded = true
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = manualAddSubExpanded,
                                        onDismissRequest = { manualAddSubExpanded = false }
                                    ) {
                                        GlobalConst.configTypeHumanFyMap.forEach { (display, configType) ->
                                            DropdownMenuItem(
                                                text = { Text(display) },
                                                onClick = {
                                                    addProfileMenuExpanded = false
                                                    manualAddSubExpanded = false

                                                    val newProfileUi =
                                                        ProfileUiState.fromProfileModel(
                                                            ProfileModel(configType = configType)
                                                        )
                                                    val id = TempStore.put(newProfileUi)
                                                    navigator.navigate(ProfileCreate(id))
                                                }
                                            )
                                        }
                                    }
                                }
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
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // SubList Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeSubId == null,
                        onClick = { viewModel.switchActiveSubId(null) },
                        label = { Text("All") }
                    )
                }
                items(subItems) { subItem ->
                    FilterChip(
                        selected = activeSubId == subItem.id,
                        onClick = { viewModel.switchActiveSubId(subItem.id) },
                        label = { Text(subItem.remark.ifBlank { "Unknown Sub" }) }
                    )
                }
            }

            // Profile List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeSubProfiles) { profile ->
                    val isActive = activeProfileId == profile.id
                    val indicatorColor = MaterialTheme.colorScheme.tertiary
                    var menuExpanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable {
                                viewModel.switchActiveProfileId(profile.id)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    if (isActive) {
                                        drawRect(
                                            color = indicatorColor,
                                            size = Size(4.dp.toPx(), size.height)
                                        )
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = profile.remark,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = MaterialTheme.typography.titleMedium.fontSize * 1.2
                                )
                                Text(
                                    text = GlobalConst.configTypeHumanFyReverseMap[profile.configType]
                                        ?: profile.configType.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = {
                                        menuExpanded = true
                                    },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_more_vert),
                                        contentDescription = "More Actions"
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Set Active") },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_check),
                                                contentDescription = "Set Active"
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.switchActiveProfileId(profile.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_share),
                                                contentDescription = "Share Profile"
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            val id = TempStore.put(profile)
                                            navigator.navigate(ProfileListShare(id))
                                        }
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val profileUiState =
                                        ProfileUiState.fromProfileModel(profile)
                                    val id = TempStore.put(profileUiState)
                                    navigator.navigate(ProfileEdit(id))
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit),
                                    contentDescription = "Edit Profile"
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteProfile(profile)
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = "Delete Profile"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}