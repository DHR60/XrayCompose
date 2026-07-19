package com.clearpath.xray_compose.ui.screen

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.enums.EngineState
import com.clearpath.xray_compose.enums.HttpDelayStatus
import com.clearpath.xray_compose.ui.theme.success
import com.clearpath.xray_compose.viewmodel.HomeViewModel

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    val viewModel: HomeViewModel = hiltViewModel()
    val rootInnerPadding = LocalRootInnerPadding.current
    val engineState by viewModel.engineStateFlow.collectAsState()
    val engineErrorMsg by viewModel.engineErrorMsgFlow.collectAsState()
    val trafficSummary by viewModel.trafficSummaryFlow.collectAsState()
    val engineHttpDelay by viewModel.engineHttpDelayFlow.collectAsState()
    val activeProfile by viewModel.activeProfileFlow.collectAsState()
    val activeEngineSetting by viewModel.activeEngineSettingFlow.collectAsState()
    val isNotificationGranted by viewModel.isNotificationPermissionGrantedFlow.collectAsStateWithLifecycle()
    val isQueryAllPackagesGranted by viewModel.isQueryAllPackagesPermissionGrantedFlow.collectAsStateWithLifecycle()
    val isAccessLocalNetworkGranted by viewModel.isAccessLocalNetworkPermissionGrantedFlow.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsMap[Manifest.permission.POST_NOTIFICATIONS]
        } else {
            true
        }?.let { viewModel.setNotificationPermissionGranted(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            permissionsMap[Manifest.permission.ACCESS_LOCAL_NETWORK]
        } else {
            true
        }?.let { viewModel.setAccessLocalNetworkPermissionGranted(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionsMap[Manifest.permission.QUERY_ALL_PACKAGES]
        } else {
            true
        }?.let { viewModel.setQueryAllPackagesPermissionGranted(it) }
    }
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.startProxyCore()
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.checkNotificationPermission()
        viewModel.checkAccessLocalNetworkPermission()
        onPauseOrDispose {}
    }

    val snackbarHostState = remember { SnackbarHostState() }
    // val scope = rememberCoroutineScope()

    LaunchedEffect(engineErrorMsg) {
        engineErrorMsg?.takeIf { it.isNotBlank() }?.let {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
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
                    if (!isNotificationGranted && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)) {
                        requestPermissionList.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    // Not necessary
                    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                    //     requestPermissionList.add(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    // }
                    if (requestPermissionList.isNotEmpty()) {
                        permissionLauncher.launch(requestPermissionList.toTypedArray())
                        return@FloatingActionButton
                    }
                    if (engineState == EngineState.STARTED) {
                        viewModel.stopProxyCore()
                    } else {
                        val vpnIntent: Intent? = VpnService.prepare(context)
                        if (vpnIntent != null) {
                            vpnLauncher.launch(vpnIntent)
                        } else {
                            viewModel.startProxyCore()
                        }
                    }
                }
            ) {
                if (activeProfile == null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_block),
                        contentDescription = stringResource(R.string.home_no_active_profile),
                    )
                } else {
                    when (engineState) {
                        EngineState.STOPPED -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_arrow),
                                contentDescription = stringResource(R.string.home_start),
                            )
                        }

                        EngineState.STARTED -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_stop),
                                contentDescription = stringResource(R.string.home_stop),
                            )
                        }

                        else -> {
                            // show loading state
                            Icon(
                                painter = painterResource(R.drawable.ic_hourglass_empty),
                                contentDescription = stringResource(R.string.home_status_loading),
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = when (engineState) {
                                EngineState.STOPPED -> stringResource(R.string.home_status_stopped)
                                EngineState.STARTED -> stringResource(R.string.home_status_running)
                                else -> stringResource(R.string.home_status_loading)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = when (engineState) {
                                EngineState.STOPPED -> MaterialTheme.colorScheme.error
                                EngineState.STARTED -> MaterialTheme.colorScheme.success
                                else -> MaterialTheme.colorScheme.outline
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.home_proxy_core_status),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = activeProfile?.remark ?: GlobalConst.none,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (activeProfile != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.home_active_profile),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = activeEngineSetting.remark.ifBlank { GlobalConst.none },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.home_active_engine_setting),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
            if (engineState == EngineState.STOPPED) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.Start,
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
                                text = if (isNotificationGranted) stringResource(R.string.home_permission_granted) else stringResource(
                                    R.string.home_permission_not_granted
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isNotificationGranted) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.home_notification_permission),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.Start,
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
                                text = when (isQueryAllPackagesGranted) {
                                    0 -> stringResource(R.string.home_tap_to_verify)
                                    1 -> stringResource(R.string.home_permission_granted)
                                    else -> stringResource(R.string.home_denied)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = when (isQueryAllPackagesGranted) {
                                    0 -> MaterialTheme.colorScheme.outline
                                    1 -> MaterialTheme.colorScheme.success
                                    else -> MaterialTheme.colorScheme.error
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.home_query_all_packages_permission),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_LOCAL_NETWORK
                                            )
                                        )
                                    }
                                }
                        ) {
                            Text(
                                text = if (isAccessLocalNetworkGranted) stringResource(R.string.home_permission_granted) else stringResource(
                                    R.string.home_permission_not_granted
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isAccessLocalNetworkGranted) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.home_access_local_network_permission),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            if (engineState == EngineState.STARTED) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.measureHttpDelay()
                        },
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val (delayText, delayColor) = when (val status = engineHttpDelay) {
                                HttpDelayStatus.NotTested -> stringResource(R.string.home_not_tested) to MaterialTheme.colorScheme.outline
                                HttpDelayStatus.Testing -> stringResource(R.string.home_testing) to MaterialTheme.colorScheme.primary
                                is HttpDelayStatus.Success -> "${status.delayMs} ms" to MaterialTheme.colorScheme.success
                                HttpDelayStatus.Timeout -> stringResource(R.string.home_timeout) to MaterialTheme.colorScheme.error
                            }
                            Text(
                                text = delayText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = delayColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.home_url_test_delay),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1: Proxy
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TrafficItem(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                label = stringResource(R.string.home_proxy_up),
                                value = longToSpeedString(trafficSummary.proxy.up.speed)
                            )
                            TrafficItem(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                label = stringResource(R.string.home_proxy_down),
                                value = longToSpeedString(trafficSummary.proxy.down.speed)
                            )
                            TrafficItem(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                label = stringResource(R.string.home_proxy_total),
                                value = longToSizeString(trafficSummary.proxy.up.total + trafficSummary.proxy.down.total)
                            )
                        }
                        // Row 2: Direct
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TrafficItem(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                label = stringResource(R.string.home_direct_up),
                                value = longToSpeedString(trafficSummary.direct.up.speed)
                            )
                            TrafficItem(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                label = stringResource(R.string.home_direct_down),
                                value = longToSpeedString(trafficSummary.direct.down.speed)
                            )
                            TrafficItem(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                label = stringResource(R.string.home_direct_total),
                                value = longToSizeString(trafficSummary.direct.up.total + trafficSummary.direct.down.total)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun longToSpeedString(value: Long): String {
    if (value <= 0) return "0 B/s"
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
    var speed = value.toDouble()
    var unitIndex = 0
    while (speed >= 1024 && unitIndex < units.size - 1) {
        speed /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) {
        "$value B/s"
    } else {
        String.format(java.util.Locale.US, "%.1f %s", speed, units[unitIndex])
    }
}

private fun longToSizeString(value: Long): String {
    if (value <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = value.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) "$value B"
    else String.format(java.util.Locale.US, "%.1f %s", size, units[unitIndex])
}
