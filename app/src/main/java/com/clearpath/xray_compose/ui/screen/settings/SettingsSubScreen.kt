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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.SettingsSubEditor
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.viewmodel.SettingsSubViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SettingsSubScreen() {
    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current

    val viewModel: SettingsSubViewModel = hiltViewModel()

    val subList by viewModel.subListFlow.collectAsState()

    val hapticFeedback = LocalHapticFeedback.current

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.reorderSubItems(from, to)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

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
                    Row {
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
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = lazyListState,
        ) {
            items(
                subList,
                key = { uiState -> uiState.config.id }
            ) { uiState ->
                ReorderableItem(
                    key = uiState.config.id,
                    state = reorderableLazyListState
                ) { isDragging ->
                    val subItem = uiState.config
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder(enabled = !isDragging)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Drag Handle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_drag_handle),
                                    contentDescription = "Drag Handle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Main Content
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = subItem.remark,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (uiState.count > 0) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = uiState.count.toString(),
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                ),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                                if (subItem.url.isNotEmpty()) {
                                    Text(
                                        text = subItem.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (subItem.lastUpdate > 1767196800000) {
                                    val lastUpdateStr = java.text.SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        java.util.Locale.US
                                    ).format(java.util.Date(subItem.lastUpdate))
                                    Text(
                                        text = "Updated: $lastUpdateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Actions
                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    IconButton(
                                        onClick = { navigator.navigate(SettingsSubEditor(subItem.id)) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_edit),
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeSubItem(subItem.id) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (subItem.url.isNotEmpty()) {
                                    if (uiState.isUpdating) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    } else {
                                        TextButton(
                                            onClick = { viewModel.updateSubForNetwork(subItem.id) },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(
                                                horizontal = 8.dp,
                                                vertical = 0.dp
                                            )
                                        ) {
                                            Text(
                                                "Update",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}