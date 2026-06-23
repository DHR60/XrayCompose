package com.clearpath.xray_compose.ui.screen.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.utils.Utils
import com.clearpath.xray_compose.viewmodel.ProfileListShareViewModel
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

@Composable
fun ProfileListShareBottomSheet(id: String) {
    val viewModel = hiltViewModel<ProfileListShareViewModel, ProfileListShareViewModel.Factory>(
        creationCallback = { factory -> factory.create(id) }
    )
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val title by viewModel.titleFlow.collectAsState()
    val shareUrl by viewModel.shareUrlFlow.collectAsState()
    val fullConfig by viewModel.fullConfigFlow.collectAsState()
    val proxyOutbounds by viewModel.proxyOutboundsFlow.collectAsState()

    val options = listOf(
        "Share URL",
        "Full Config",
        "Proxy Outbounds"
    )
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(100.dp)),
            indicator = {},
            divider = {},
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Tab(
                    selected = isSelected,
                    onClick = { selectedIndex = index },
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        ),
                    text = {
                        Text(
                            text = option,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val content = viewModel.getContentForIndex(selectedIndex)
                    if (content.isNotEmpty()) {
                        Utils.setClipboard(context, content)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_content_copy),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Copy")
            }
            Button(
                onClick = {
                    val content = viewModel.getContentForIndex(selectedIndex)
                    if (content.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, content)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share via"))
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Share")
            }
        }
        SelectionContainer(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedIndex) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .aspectRatio(1f)
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberQrCodePainter(shareUrl),
                                    contentDescription = "Share URL QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = shareUrl,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    1 -> {
                        Text(
                            text = fullConfig,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    2 -> {
                        Text(
                            text = proxyOutbounds,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
