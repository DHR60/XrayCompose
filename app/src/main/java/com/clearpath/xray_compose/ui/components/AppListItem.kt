package com.clearpath.xray_compose.ui.components

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.clearpath.xray_compose.viewmodel.uistate.AppItemInfo

@Composable
fun AppListItem(
    appItemInfo: AppItemInfo,
    isSelected: Boolean,
    iconCacheMap: Map<String, Drawable>,
    fetchAppIcon: (ApplicationInfo) -> Unit,
    onToggleSelection: () -> Unit
) {
    val cachedIcon = iconCacheMap[appItemInfo.packageName]

    LaunchedEffect(appItemInfo.packageName) {
        if (cachedIcon == null) {
            fetchAppIcon(appItemInfo.applicationInfo)
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