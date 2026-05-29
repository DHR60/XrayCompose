package com.clearpath.xray_compose.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.viewmodel.HomeViewModel

@Composable
fun HomeScreen() {
    val viewModel: HomeViewModel = viewModel()
    val rootInnerPadding = LocalRootInnerPadding.current
    val isProxyCoreRunning by viewModel.isProxyCoreRunningFlow.collectAsState()
    val activeProfile by viewModel.activeProfileFlow.collectAsState()
    val activeEngineSetting by viewModel.activeEngineSettingFlow.collectAsState()
    val isNotificationGranted by viewModel.isNotificationPermissionGrantedFlow.collectAsStateWithLifecycle()
    val isQueryAllPackagesGranted by viewModel.isQueryAllPackagesPermissionGrantedFlow.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsMap[Manifest.permission.POST_NOTIFICATIONS]
        } else {
            true
        }?.let { viewModel.setNotificationPermissionGranted(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionsMap[Manifest.permission.QUERY_ALL_PACKAGES]
        } else {
            true
        }?.let { viewModel.setQueryAllPackagesPermissionGranted(it) }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.checkNotificationPermission()
        onPauseOrDispose {}
    }
    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        topBar = {
            TopAppBar(
                title = { Text("Xray Compose") },
                windowInsets = WindowInsets(0.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeProfile == null) {
                        return@FloatingActionButton
                    }
                    val requestPermissionList = mutableListOf<String>()
                    if (!isNotificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionList.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (requestPermissionList.isNotEmpty()) {
                        permissionLauncher.launch(requestPermissionList.toTypedArray())
                        return@FloatingActionButton
                    }
                    if (isProxyCoreRunning == 1) {
                        viewModel.stopProxyCore()
                    } else if (isProxyCoreRunning == 0) {
                        viewModel.startProxyCore()
                    }
                }
            ) {
                if (activeProfile == null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_block),
                        contentDescription = "No active profile",
                    )
                } else {
                    when (isProxyCoreRunning) {
                        0 -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_arrow),
                                contentDescription = "Start",
                            )
                        }

                        1 -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_stop),
                                contentDescription = "Stop",
                            )
                        }

                        else -> {
                            // show loading state
                            Icon(
                                painter = painterResource(R.drawable.ic_hourglass_empty),
                                contentDescription = "Loading",
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Proxy Core Status:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = when (isProxyCoreRunning) {
                                0 -> "Stopped"
                                1 -> "Running"
                                else -> "Loading..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = when (isProxyCoreRunning) {
                                0 -> Color.Red
                                1 -> Color.Green
                                else -> Color.Gray
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Active Profile:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = activeProfile?.remark ?: GlobalConst.none,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (activeProfile != null) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Active Engine Setting:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = activeEngineSetting.remark.ifBlank { GlobalConst.none },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    )
                                }
                            }
                    ) {
                        Text(
                            text = "Notification Permission:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isNotificationGranted) "Granted" else "Not Granted",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isNotificationGranted) Color.Green else Color.Red
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // NOTE: some rom (e.g. MIUI) may enable "pseudo permission" for QUERY_ALL_PACKAGES, so we need:
                                // 1. check if manifest permission is granted, if not, request it;
                                // 2. if manifest permission is granted, check if it is really granted
                                // 3. The first check usually fails. After the first check, the system automatically pop up the permission request dialog.
                                // 4. After user grant or deny, we can check the real permission result in the second check.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.QUERY_ALL_PACKAGES
                                        )
                                    )
                                }
                                viewModel.checkQueryAllPackagesPermissionReallyGranted()
                            }
                    ) {
                        Text(
                            text = "Query All Packages Permission:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = when (isQueryAllPackagesGranted) {
                                0 -> "Tap to verify"
                                1 -> "Granted"
                                else -> "Denied"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = when (isQueryAllPackagesGranted) {
                                0 -> Color.Gray
                                1 -> Color.Green
                                else -> Color.Red
                            }
                        )
                    }
                }
            }
        }
    }
}