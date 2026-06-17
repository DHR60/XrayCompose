package com.clearpath.xray_compose.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.tempstore.TempStore
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.ProfileCreate
import com.clearpath.xray_compose.ui.navigation.ProfileEdit
import com.clearpath.xray_compose.ui.navigation.ProfileListShare
import com.clearpath.xray_compose.ui.theme.AppAnimation
import com.clearpath.xray_compose.viewmodel.ProfileListViewModel
import com.clearpath.xray_compose.viewmodel.uistate.ProfileUiState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ProfileListScreen() {
    val viewModel: ProfileListViewModel = hiltViewModel()
    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current
    val subItems by viewModel.subItemsFlow.collectAsState()
    val activeSubId by viewModel.activeSubIdFlow.collectAsState()
    val activeProfileId by viewModel.activeProfileIdFlow.collectAsState()
    val profilesWithTest by viewModel.profilesWithTestFlow.collectAsState()
    val isTesting by viewModel.isTestingFlow.collectAsState()
    val testProgress by viewModel.testProgressFlow.collectAsState()
    val isBusy by viewModel.isBusyFlow.collectAsState()
    val errorMessage by viewModel.errorMessageFlow.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val hapticFeedback = LocalHapticFeedback.current

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.reorderProfiles(from, to)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.takeIf { it.isNotBlank() }?.let {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val allSubIds = remember(subItems) { listOf(null) + subItems.map { it.id } }

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile List") },
                actions = {
                    Row {
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
                        val moreExpanded = remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = {
                                    moreExpanded.value = true
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert),
                                    contentDescription = "More Actions"
                                )
                            }
                            DropdownMenu(
                                expanded = moreExpanded.value,
                                onDismissRequest = { moreExpanded.value = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Test Real Delay") },
                                    onClick = {
                                        moreExpanded.value = false
                                        viewModel.testCurrentSubProfiles()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Update Subscription") },
                                    onClick = {
                                        moreExpanded.value = false
                                        viewModel.updateSubForNetwork()
                                    }
                                )
                                if (isTesting) {
                                    DropdownMenuItem(
                                        text = { Text("Stop Testing") },
                                        onClick = {
                                            moreExpanded.value = false
                                            viewModel.stopTesting()
                                        }
                                    )
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
            if (isTesting) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val progress = if (testProgress.total > 0) {
                        testProgress.current.toFloat() / testProgress.total.toFloat()
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Testing: ${testProgress.current} / ${testProgress.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }

            // SubList Row
            val selectedTabIndex = allSubIds.indexOf(activeSubId).coerceAtLeast(0)
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp,
                divider = {}
            ) {
                allSubIds.forEach { subId ->
                    Tab(
                        selected = activeSubId == subId,
                        onClick = { viewModel.switchActiveSubId(subId) },
                        text = {
                            Text(
                                text = if (subId == null) "All"
                                else subItems.find { it.id == subId }?.remark?.ifBlank { "Unknown Sub" }
                                    ?: "Unknown Sub",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                }
            }

            // Profile List
            AnimatedContent(
                targetState = activeSubId,
                label = "ProfileListTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    val initialIdx = allSubIds.indexOf(initialState)
                    val targetIdx = allSubIds.indexOf(targetState)
                    val baseTransition = if (targetIdx > initialIdx) {
                        AppAnimation.ForwardTransition
                    } else {
                        AppAnimation.PopTransition
                    }
                    baseTransition.using(null)
                }
            ) { targetSubId ->
                val profiles = profilesWithTest[targetSubId] ?: emptyList()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = lazyListState,
                ) {
                    items(profiles, key = { it.profile.id }) { item ->
                        ReorderableItem(
                            state = reorderableLazyListState,
                            key = item.profile.id
                        ) { isDragging ->
                            val profile = item.profile
                            val test = item.test
                            val isActive = activeProfileId == profile.id
                            var menuExpanded by remember { mutableStateOf(false) }

                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .clickable {
                                        viewModel.switchActiveProfileId(profile.id)
                                    },
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer
                                    else if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isActive) BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                                else CardDefaults.outlinedCardBorder(enabled = !isDragging)
                            ) {
                                ListItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_drag_handle),
                                            contentDescription = "Drag Handle",
                                            modifier = Modifier
                                                .size(20.dp)
                                                .longPressDraggableHandle(
                                                    onDragStarted = {
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.GestureThresholdActivate
                                                        )
                                                    },
                                                    onDragStopped = {
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.GestureEnd
                                                        )
                                                    },
                                                ),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.6f
                                            )
                                        )
                                    },
                                    headlineContent = {
                                        Text(
                                            text = profile.remark,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    supportingContent = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = GlobalConst.configTypeHumanFyReverseMap[profile.configType]
                                                    ?: profile.configType.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                            )
                                            if (test != null) {
                                                Text(
                                                    text = if (test.delay > 0) "${test.delay} ms" else if (test.message.contains(
                                                            ": "
                                                        )
                                                    ) test.message.substringAfterLast(": ") else test.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (test.delay > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(start = 8.dp)
                                                )
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                modifier = Modifier.size(40.dp),
                                                onClick = {
                                                    val profileUiState =
                                                        ProfileUiState.fromProfileModel(profile)
                                                    val id = TempStore.put(profileUiState)
                                                    navigator.navigate(ProfileEdit(id))
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_edit),
                                                    contentDescription = "Edit Profile",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Box {
                                                IconButton(
                                                    modifier = Modifier.size(40.dp),
                                                    onClick = {
                                                        menuExpanded = true
                                                    }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_more_vert),
                                                        contentDescription = "More Actions",
                                                        modifier = Modifier.size(20.dp)
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
                                                    DropdownMenuItem(
                                                        text = { Text("Test Real Ping Delay") },
                                                        leadingIcon = {
                                                            Icon(
                                                                painter = painterResource(R.drawable.ic_network_ping),
                                                                contentDescription = "Test Profile"
                                                            )
                                                        },
                                                        onClick = {
                                                            menuExpanded = false
                                                            viewModel.testProfile(profile)
                                                        }
                                                    )
                                                    HorizontalDivider()
                                                    DropdownMenuItem(
                                                        text = { Text("Delete") },
                                                        leadingIcon = {
                                                            Icon(
                                                                painter = painterResource(R.drawable.ic_delete),
                                                                contentDescription = "Delete Profile"
                                                            )
                                                        },
                                                        onClick = {
                                                            menuExpanded = false
                                                            viewModel.deleteProfile(profile)
                                                        }
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
        }
    }
}
