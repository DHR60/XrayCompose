package com.clearpath.xray_compose.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.viewmodel.ProfileSelectorViewModel

@Composable
fun ProfileSelector(
    onProfileClicked: (ProfileModel) -> Unit,
    modifier: Modifier = Modifier,
    selectedProfileIds: List<String> = emptyList(),
    selectedProfileRemarks: List<String> = emptyList()
) {
    val viewModel: ProfileSelectorViewModel = hiltViewModel()
    val subItems by viewModel.subItemsFlow.collectAsState()
    val activeSubId by viewModel.activeSubIdFlow.collectAsState()
    val activeProfileId by viewModel.activeProfileIdFlow.collectAsState()
    val profilesWithTest by viewModel.profilesWithTestFlow.collectAsState()

    val lazyListState = rememberLazyListState()

    val allSubIds = remember(subItems) { listOf(null) + subItems.map { it.id } }

    Column(
        modifier = modifier
    ) {
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
                    onClick = { viewModel.switchSubId(subId) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = lazyListState,
        ) {
            items(profilesWithTest, key = { it.profile.id }) { item ->
                val profile = item.profile
                val test = item.test
                val isActive = activeProfileId == profile.id
                val isSelected =
                    selectedProfileIds.contains(profile.id) || selectedProfileRemarks.contains(
                        profile.remark
                    )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clickable {
                            onProfileClicked(profile)
                        },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer
                        else if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isActive) BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.primary
                    )
                    else CardDefaults.outlinedCardBorder(enabled = !isSelected)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.remark,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
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
                        }
                    }
                }
            }
        }
    }
}