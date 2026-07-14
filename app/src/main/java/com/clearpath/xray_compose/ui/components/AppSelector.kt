package com.clearpath.xray_compose.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.clearpath.xray_compose.viewmodel.AppSelectorViewModel
import com.clearpath.xray_compose.viewmodel.uistate.AppItemInfo

@Composable
fun AppSelector(
    onSelectedChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    selectedPackageName: List<String> = emptyList(),
    multiSelect: Boolean = true
) {
    val viewModel: AppSelectorViewModel = hiltViewModel()

    val isBusy by viewModel.isBusyFlow.collectAsState()
    val displayAppPackages by viewModel.displayAppPackagesFlow.collectAsState()
    val searchQuery by viewModel.searchQueryFlow.collectAsState()

    LaunchedEffect(selectedPackageName) {
        viewModel.setSelectedPackages(selectedPackageName)
    }

    Column(
        modifier = modifier
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
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search apps...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
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
                    isSelected = selectedPackageName.contains(item.packageName),
                    viewModel = viewModel,
                    onToggleSelection = {
                        if (!multiSelect) {
                            onSelectedChanged(listOf(item.packageName))
                        } else {
                            val currentSelected = selectedPackageName.toMutableList()
                            if (currentSelected.contains(item.packageName)) {
                                currentSelected.remove(item.packageName)
                            } else {
                                currentSelected.add(item.packageName)
                            }
                            onSelectedChanged(currentSelected)
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun AppListItem(
    appItemInfo: AppItemInfo,
    isSelected: Boolean,
    viewModel: AppSelectorViewModel,
    onToggleSelection: () -> Unit
) {
    val cachedIcon = viewModel.iconCache[appItemInfo.packageName]

    LaunchedEffect(appItemInfo.packageName) {
        if (cachedIcon == null) {
            viewModel.fetchAppIcon(appItemInfo.applicationInfo)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cachedIcon)
                    .crossfade(true)
                    .build(),
                contentDescription = appItemInfo.appName,
                modifier = Modifier
                    .size(40.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = appItemInfo.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = appItemInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )
        }
    }
}