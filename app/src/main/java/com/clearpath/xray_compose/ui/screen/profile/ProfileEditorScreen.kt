package com.clearpath.xray_compose.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.enums.EConfigType
import com.clearpath.xray_compose.enums.ETransport
import com.clearpath.xray_compose.ui.components.EditableTrailingIconField
import com.clearpath.xray_compose.ui.components.FormBottomSheetContext
import com.clearpath.xray_compose.ui.components.ReusableFormBottomSheet
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.viewmodel.ProfileEditorViewModel
import kotlinx.coroutines.launch
import kotlin.enums.enumEntries

@Composable
fun ProfileEditorScreen(
    id: String,
    isNew: Boolean = false,
) {
    // NOTE: VLESS only
    // TODO: expand to support other types of profiles
    val navigator = LocalNavigator.current
    val rootInnerPadding = LocalRootInnerPadding.current

    val viewModel = hiltViewModel<ProfileEditorViewModel, ProfileEditorViewModel.Factory>(
        creationCallback = { factory -> factory.create(id) }
    )

    // val profileUiState by viewModel.uiState.collectAsState()
    val profileModel by viewModel.profileModel.collectAsState()
    val protoExtra by viewModel.protoExtra.collectAsState()
    val transportExtra by viewModel.transportExtra.collectAsState()

    val transportNetwork by viewModel.transportNetwork.collectAsState()

    var activeDialogContext by remember { mutableStateOf<FormBottomSheetContext?>(null) }

    val portEmptyMsg = stringResource(R.string.validator_port_empty)
    val portDigitsMsg = stringResource(R.string.validator_port_digits)
    val portRangeMsg = stringResource(R.string.validator_port_range)

    // Port validator
    val portValidator: (String) -> String? = { value ->
        when {
            value.isEmpty() -> portEmptyMsg
            !value.all { it.isDigit() } -> portDigitsMsg
            value.toIntOrNull()
                ?.let { it !in 1..65535 } != false -> portRangeMsg

            else -> null
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isTransportMethodsEnabled =
        profileModel.configType != EConfigType.WIREGUARD && profileModel.configType != EConfigType.HYSTERIA2
    val isTransportSecurityEnabled =
        profileModel.configType != EConfigType.WIREGUARD && profileModel.configType != EConfigType.SOCKS5
    val isRealityEnabled =
        isTransportSecurityEnabled && profileModel.configType != EConfigType.HYSTERIA2

    val deleteErrorMsgPrefix = stringResource(R.string.editor_error_delete)
    val saveErrorMsgPrefix = stringResource(R.string.editor_error_save)

    val editorRemarkLabel = stringResource(R.string.editor_remark)
    val editorAddressLabel = stringResource(R.string.editor_address)
    val editorPortLabel = stringResource(R.string.editor_port)
    val editorEditPrefix = stringResource(R.string.editor_edit_prefix)

    val editorIdLabel = stringResource(R.string.editor_id)
    val editorFlowLabel = stringResource(R.string.editor_flow)
    val editorEncryptionLabel = stringResource(R.string.editor_encryption)
    val editorPasswordLabel = stringResource(R.string.editor_password)
    val editorSalamanderPasswordLabel = stringResource(R.string.editor_salamander_password)
    val editorBandwidthUpLabel = stringResource(R.string.editor_bandwidth_up)
    val editorBandwidthDownLabel = stringResource(R.string.editor_bandwidth_down)
    val editorPortHoppingLabel = stringResource(R.string.editor_port_hopping)
    val editorHopIntervalLabel = stringResource(R.string.editor_hop_interval)
    val editorTransportMethodsLabel = stringResource(R.string.editor_transport_methods)
    val editorXhttpModeLabel = stringResource(R.string.editor_xhttp_mode)
    val editorXhttpHostLabel = stringResource(R.string.editor_xhttp_host)
    val editorXhttpPathLabel = stringResource(R.string.editor_xhttp_path)
    val editorXhttpExtraLabel = stringResource(R.string.editor_xhttp_extra)
    val editorHostLabel = stringResource(R.string.editor_host)
    val editorPathLabel = stringResource(R.string.editor_path)
    val editorGrpcModeLabel = stringResource(R.string.editor_grpc_mode)
    val editorGrpcAuthorityLabel = stringResource(R.string.editor_grpc_authority)
    val editorGrpcServiceNameLabel = stringResource(R.string.editor_grpc_service_name)
    val editorKcpHeaderLabel = stringResource(R.string.editor_kcp_header)
    val editorKcpSeedLabel = stringResource(R.string.editor_kcp_seed)
    val editorTransportSecurityLabel = stringResource(R.string.editor_transport_security)
    val editorSniLabel = stringResource(R.string.editor_sni)
    val editorUtlsLabel = stringResource(R.string.editor_utls)
    val editorAlpnLabel = stringResource(R.string.editor_alpn)
    val editorAllowInsecureLabel = stringResource(R.string.editor_allow_insecure)
    val editorEchConfigLabel = stringResource(R.string.editor_ech_config)
    val editorClientCertLabel = stringResource(R.string.editor_client_cert)
    val editorVerifyPeerNameLabel = stringResource(R.string.editor_verify_peer_name)
    val editorCertShaLabel = stringResource(R.string.editor_cert_sha)
    val editorRealityPublicKeyLabel = stringResource(R.string.editor_reality_public_key)
    val editorRealityShortIdLabel = stringResource(R.string.editor_reality_short_id)
    val editorRealitySpiderXLabel = stringResource(R.string.editor_reality_spider_x)
    val editorRealityMldsa65Label = stringResource(R.string.editor_reality_mldsa65)
    val editorFinalmaskLabel = stringResource(R.string.editor_finalmask)
    val editorSelectFlow = stringResource(R.string.editor_select_flow)
    val editorSelectTransport = stringResource(R.string.editor_select_transport)
    val editorSelectSecurity = stringResource(R.string.editor_select_security)
    val editorSelectUtls = stringResource(R.string.editor_select_utls)
    val editorSelectAlpn = stringResource(R.string.editor_select_alpn)
    val editorSelectAllowInsecure = stringResource(R.string.editor_select_allow_insecure)

    Scaffold(
        modifier = Modifier.padding(rootInnerPadding),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(GlobalConst.configTypeHumanFyReverseMap[profileModel.configType]!!) },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.logcat_back)
                        )
                    }
                },
                actions = {
                    Row {
                        if (!isNew) {
                            IconButton(onClick = {
                                viewModel.deleteProfile(onSuccess = {
                                    navigator.goBack()
                                }, onError = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            deleteErrorMsgPrefix.format(
                                                it
                                            )
                                        )
                                    }
                                })
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.editor_delete)
                                )
                            }
                        }
                        IconButton(onClick = {
                            viewModel.saveProfile(onSuccess = {
                                navigator.goBack()
                            }, onError = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(saveErrorMsgPrefix.format(it))
                                }
                            })
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_save),
                                contentDescription = stringResource(R.string.editor_save)
                            )
                        }
                        // TextButton(
                        //     onClick = {
                        //         scope.launch {
                        //             snackbarHostState.showSnackbar("Proto Extra: $protoExtra\nTransport Extra: $transportExtra")
                        //         }
                        //     }
                        // ) {
                        //     Text("Test")
                        // }
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { FormSectionHeader(title = stringResource(R.string.editor_basic_settings)) }
            item {
                FormCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        EditableTrailingIconField(
                            value = profileModel.remark,
                            onValueChange = { newRemark ->
                                viewModel.updateProfileModel { currentState ->
                                    currentState.copy(remark = newRemark)
                                }
                            },
                            label = { Text(editorRemarkLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            onEditIconClick = {
                                activeDialogContext = FormBottomSheetContext(
                                    fieldKey = "remark",
                                    title = editorEditPrefix.format(editorRemarkLabel),
                                    initialValue = profileModel.remark,
                                    onConfirm = { newRemark ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(remark = newRemark)
                                        }
                                    }
                                )
                            }
                        )
                        EditableTrailingIconField(
                            value = profileModel.address,
                            onValueChange = { newValue ->
                                viewModel.updateProfileModel { currentState ->
                                    currentState.copy(address = newValue)
                                }
                            },
                            label = { Text(editorAddressLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            onEditIconClick = {
                                activeDialogContext = FormBottomSheetContext(
                                    fieldKey = "address",
                                    title = editorEditPrefix.format(editorAddressLabel),
                                    initialValue = profileModel.address,
                                    onConfirm = { newAddress ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(address = newAddress)
                                        }
                                    },
                                )
                            }
                        )
                        EditableTrailingIconField(
                            value = profileModel.port.toString(),
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    viewModel.updateProfileModel { currentState ->
                                        currentState.copy(
                                            port = newValue.toIntOrNull() ?: currentState.port
                                        )
                                    }
                                }
                            },
                            label = { Text(editorPortLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            onEditIconClick = {
                                activeDialogContext = FormBottomSheetContext(
                                    fieldKey = "port",
                                    title = editorEditPrefix.format(editorPortLabel),
                                    initialValue = profileModel.port.toString(),
                                    onConfirm = { newPort ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(
                                                port = newPort.toIntOrNull() ?: currentState.port
                                            )
                                        }
                                    },
                                    validator = portValidator
                                )
                            }
                        )
                    }
                }
            }
            if (profileModel.configType == EConfigType.VLESS) {
                item { FormSectionHeader(title = stringResource(R.string.editor_vless_settings)) }
                item {
                    FormCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EditableTrailingIconField(
                                value = profileModel.password,
                                onValueChange = { newValue ->
                                    viewModel.updateProfileModel { currentState ->
                                        currentState.copy(password = newValue)
                                    }
                                },
                                label = { Text(editorIdLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "id",
                                        title = editorEditPrefix.format(editorIdLabel),
                                        initialValue = profileModel.password,
                                        onConfirm = { newId ->
                                            viewModel.updateProfileModel { currentState ->
                                                currentState.copy(password = newId)
                                            }
                                        }
                                    )
                                }
                            )
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = if (protoExtra.flow.isNullOrEmpty()) editorSelectFlow else protoExtra.flow!!,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(editorFlowLabel) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    trailingIcon = {
                                        TrailingIcon(
                                            expanded = expanded
                                        )
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                        unfocusedTextColor = if (protoExtra.flow.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    GlobalConst.vlessFlowList.forEach { flowOption ->
                                        DropdownMenuItem(
                                            text = { Text(flowOption) },
                                            onClick = {
                                                viewModel.updateProtocolExtra { currentState ->
                                                    currentState.copy(flow = flowOption)
                                                }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            EditableTrailingIconField(
                                value = if (protoExtra.vlessEncryption.isNullOrEmpty()) GlobalConst.none else protoExtra.vlessEncryption!!,
                                onValueChange = { newValue ->
                                    viewModel.updateProtocolExtra { currentState ->
                                        currentState.copy(vlessEncryption = newValue)
                                    }
                                },
                                label = { Text(editorEncryptionLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "vlessEncryption",
                                        title = editorEditPrefix.format(editorEncryptionLabel),
                                        initialValue = if (protoExtra.vlessEncryption.isNullOrEmpty()) GlobalConst.none else protoExtra.vlessEncryption!!,
                                        onConfirm = { newEncryption ->
                                            viewModel.updateProtocolExtra { currentState ->
                                                currentState.copy(vlessEncryption = newEncryption)
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            } else if (profileModel.configType == EConfigType.HYSTERIA2) {
                item { FormSectionHeader(title = stringResource(R.string.editor_hysteria2_settings)) }
                item {
                    FormCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EditableTrailingIconField(
                                value = profileModel.password,
                                onValueChange = { newValue ->
                                    viewModel.updateProfileModel { currentState ->
                                        currentState.copy(password = newValue)
                                    }
                                },
                                label = { Text(editorPasswordLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "hysteria2Password",
                                        title = editorEditPrefix.format(editorPasswordLabel),
                                        initialValue = profileModel.password,
                                        onConfirm = { newValue ->
                                            viewModel.updateProfileModel { currentState ->
                                                currentState.copy(password = newValue)
                                            }
                                        }
                                    )
                                }
                            )
                            EditableTrailingIconField(
                                value = protoExtra.salamanderPass ?: "",
                                onValueChange = { newValue ->
                                    viewModel.updateProtocolExtra { currentState ->
                                        currentState.copy(salamanderPass = newValue)
                                    }
                                },
                                label = { Text(editorSalamanderPasswordLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "hysteria2SalamanderPassword",
                                        title = editorEditPrefix.format(
                                            editorSalamanderPasswordLabel
                                        ),
                                        initialValue = protoExtra.salamanderPass ?: "",
                                        onConfirm = { newValue ->
                                            viewModel.updateProtocolExtra { currentState ->
                                                currentState.copy(salamanderPass = newValue)
                                            }
                                        }
                                    )
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                EditableTrailingIconField(
                                    value = protoExtra.upMbps ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateProtocolExtra { currentState ->
                                            currentState.copy(upMbps = newValue)
                                        }
                                    },
                                    label = { Text(editorBandwidthUpLabel) },
                                    modifier = Modifier.weight(1f),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "hysteria2UpMbps",
                                            title = editorEditPrefix.format(editorBandwidthUpLabel),
                                            initialValue = protoExtra.upMbps ?: "",
                                            onConfirm = { newValue ->
                                                viewModel.updateProtocolExtra { currentState ->
                                                    currentState.copy(upMbps = newValue)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = protoExtra.downMbps ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateProtocolExtra { currentState ->
                                            currentState.copy(downMbps = newValue)
                                        }
                                    },
                                    label = { Text(editorBandwidthDownLabel) },
                                    modifier = Modifier.weight(1f),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "hysteria2DownMbps",
                                            title = editorEditPrefix.format(editorBandwidthDownLabel),
                                            initialValue = protoExtra.downMbps ?: "",
                                            onConfirm = { newValue ->
                                                viewModel.updateProtocolExtra { currentState ->
                                                    currentState.copy(downMbps = newValue)
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                            EditableTrailingIconField(
                                value = protoExtra.ports ?: "",
                                onValueChange = { newValue ->
                                    viewModel.updateProtocolExtra { currentState ->
                                        currentState.copy(ports = newValue)
                                    }
                                },
                                label = { Text(editorPortHoppingLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "hysteria2Ports",
                                        title = editorEditPrefix.format(editorPortHoppingLabel),
                                        initialValue = protoExtra.ports ?: "",
                                        onConfirm = { newValue ->
                                            viewModel.updateProtocolExtra { currentState ->
                                                currentState.copy(ports = newValue)
                                            }
                                        }
                                    )
                                }
                            )
                            EditableTrailingIconField(
                                value = protoExtra.hopInterval ?: "",
                                onValueChange = { newValue ->
                                    viewModel.updateProtocolExtra { currentState ->
                                        currentState.copy(hopInterval = newValue)
                                    }
                                },
                                label = { Text(editorHopIntervalLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "hysteria2PortsHoppingInterval",
                                        title = editorEditPrefix.format(editorHopIntervalLabel),
                                        initialValue = protoExtra.hopInterval ?: "",
                                        onConfirm = { newValue ->
                                            viewModel.updateProtocolExtra { currentState ->
                                                currentState.copy(hopInterval = newValue)
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            } else if (profileModel.configType == EConfigType.TROJAN) {
                item { FormSectionHeader(title = stringResource(R.string.editor_trojan_settings)) }
                item {
                    FormCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EditableTrailingIconField(
                                value = profileModel.password,
                                onValueChange = { newValue ->
                                    viewModel.updateProfileModel { currentState ->
                                        currentState.copy(password = newValue)
                                    }
                                },
                                label = { Text("ID") },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "id",
                                        title = "Edit ID",
                                        initialValue = profileModel.password,
                                        onConfirm = { newId ->
                                            viewModel.updateProfileModel { currentState ->
                                                currentState.copy(password = newId)
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
            if (isTransportMethodsEnabled) {
                item { FormSectionHeader(title = stringResource(R.string.editor_transport_methods_settings)) }
                item {
                    FormCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = transportNetwork.value,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(editorTransportMethodsLabel) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    trailingIcon = {
                                        TrailingIcon(
                                            expanded = expanded
                                        )
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    enumEntries<ETransport>().forEach { networkOption ->
                                        DropdownMenuItem(
                                            text = { Text(networkOption.value) },
                                            onClick = {
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(network = networkOption.value)
                                                }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (transportNetwork == ETransport.RAW) {
                                // do nothing
                            } else if (transportNetwork == ETransport.XHTTP) {
                                var xhttpExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = xhttpExpanded,
                                    onExpandedChange = { xhttpExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = if (transportExtra.xhttpMode.isNullOrEmpty()) GlobalConst.defaultXhttpMode else transportExtra.xhttpMode!!,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(editorXhttpModeLabel) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                        trailingIcon = {
                                            TrailingIcon(
                                                expanded = xhttpExpanded
                                            )
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = xhttpExpanded,
                                        onDismissRequest = { xhttpExpanded = false }
                                    ) {
                                        GlobalConst.xhttpModeList.forEach { xhttpOption ->
                                            DropdownMenuItem(
                                                text = { Text(xhttpOption) },
                                                onClick = {
                                                    viewModel.updateTransportExtra { currentState ->
                                                        currentState.copy(xhttpMode = xhttpOption)
                                                    }
                                                    xhttpExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                EditableTrailingIconField(
                                    value = transportExtra.host ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(host = newValue)
                                        }
                                    },
                                    label = { Text(editorXhttpHostLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "xhttpHost",
                                            title = editorEditPrefix.format(editorXhttpHostLabel),
                                            initialValue = transportExtra.host ?: "",
                                            onConfirm = { newHost ->
                                                viewModel.updateTransportExtra { currentState ->
                                                    currentState.copy(host = newHost)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = transportExtra.path ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(path = newValue)
                                        }
                                    },
                                    label = { Text(editorXhttpPathLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "xhttpPath",
                                            title = editorEditPrefix.format(editorXhttpPathLabel),
                                            initialValue = transportExtra.path ?: "",
                                            onConfirm = { newPath ->
                                                viewModel.updateTransportExtra { currentState ->
                                                    currentState.copy(path = newPath)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = transportExtra.xhttpExtra ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(xhttpExtra = newValue)
                                        }
                                    },
                                    label = { Text(editorXhttpExtraLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "xhttpExtra",
                                            title = editorEditPrefix.format(editorXhttpExtraLabel),
                                            initialValue = transportExtra.xhttpExtra ?: "",
                                            onConfirm = { newExtra ->
                                                viewModel.updateTransportExtra { currentState ->
                                                    currentState.copy(xhttpExtra = newExtra)
                                                }
                                            }
                                        )
                                    }
                                )
                            } else if (transportNetwork == ETransport.WS
                                || transportNetwork == ETransport.HTTPUPGRADE
                            ) {
                                EditableTrailingIconField(
                                    value = transportExtra.host ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(host = newValue)
                                        }
                                    },
                                    label = { Text(editorHostLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "host",
                                            title = editorEditPrefix.format(editorHostLabel),
                                            initialValue = transportExtra.host ?: "",
                                            onConfirm = { newHost ->
                                                viewModel.updateTransportExtra { currentState ->
                                                    currentState.copy(host = newHost)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = transportExtra.path ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(path = newValue)
                                        }
                                    },
                                    label = { Text(editorPathLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "path",
                                            title = editorEditPrefix.format(editorPathLabel),
                                            initialValue = transportExtra.path ?: "",
                                            onConfirm = { newPath ->
                                                viewModel.updateTransportExtra { currentState ->
                                                    currentState.copy(path = newPath)
                                                }
                                            }
                                        )
                                    }
                                )
                            } else if (transportNetwork == ETransport.GRPC) {
                                var grpcModeExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = grpcModeExpanded,
                                    onExpandedChange = { grpcModeExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = if (transportExtra.grpcMode.isNullOrEmpty()) GlobalConst.defaultGrpcMode else transportExtra.grpcMode!!,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(editorGrpcModeLabel) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                        trailingIcon = {
                                            TrailingIcon(
                                                expanded = grpcModeExpanded
                                            )
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = grpcModeExpanded,
                                        onDismissRequest = { grpcModeExpanded = false }
                                    ) {
                                        GlobalConst.grpcModeList.forEach { grpcOption ->
                                            DropdownMenuItem(
                                                text = { Text(grpcOption) },
                                                onClick = {
                                                    viewModel.updateTransportExtra { currentState ->
                                                        currentState.copy(grpcMode = grpcOption)
                                                    }
                                                    grpcModeExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                EditableTrailingIconField(
                                    value = transportExtra.grpcAuthority ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(grpcAuthority = newValue)
                                        }
                                    },
                                    label = { Text(editorGrpcAuthorityLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "authority",
                                            title = editorEditPrefix.format(editorGrpcAuthorityLabel),
                                            initialValue = transportExtra.grpcAuthority ?: "",
                                            onConfirm = { newHost ->
                                                viewModel.updateTransportExtra { currentState ->
                                                    currentState.copy(grpcAuthority = newHost)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = transportExtra.grpcServiceName ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(grpcServiceName = newValue)
                                        }
                                    },
                                    label = { Text(editorGrpcServiceNameLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "serviceName",
                                            title = editorEditPrefix.format(
                                                editorGrpcServiceNameLabel
                                            ),
                                            initialValue = transportExtra.grpcServiceName ?: "",
                                            onConfirm = { newPath ->
                                                viewModel.updateTransportExtra { currentState ->
                                                    currentState.copy(grpcServiceName = newPath)
                                                }
                                            }
                                        )
                                    }
                                )
                            } else if (transportNetwork == ETransport.KCP) {
                                var kcpHeaderExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = kcpHeaderExpanded,
                                    onExpandedChange = { kcpHeaderExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = if (transportExtra.kcpSeed.isNullOrEmpty()) "Select KCP Header" else transportExtra.kcpSeed!!,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(editorKcpHeaderLabel) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                        trailingIcon = {
                                            TrailingIcon(
                                                expanded = kcpHeaderExpanded
                                            )
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                            unfocusedTextColor = if (transportExtra.kcpSeed.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = kcpHeaderExpanded,
                                        onDismissRequest = { kcpHeaderExpanded = false }
                                    ) {
                                        GlobalConst.kcpHeaderMap.forEach { kcpHeaderOption ->
                                            DropdownMenuItem(
                                                text = { Text(kcpHeaderOption.key) },
                                                onClick = {
                                                    viewModel.updateTransportExtra { currentState ->
                                                        currentState.copy(kcpHeaderType = kcpHeaderOption.key)
                                                    }
                                                    kcpHeaderExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                EditableTrailingIconField(
                                    value = transportExtra.kcpSeed ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(kcpSeed = newValue)
                                        }
                                    },
                                    label = { Text(editorKcpSeedLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "seed",
                                            title = editorEditPrefix.format(editorKcpSeedLabel),
                                            initialValue = transportExtra.kcpSeed ?: "",
                                            onConfirm = { newHost ->
                                                viewModel.updateTransportExtra { currentState ->
                                                    currentState.copy(kcpSeed = newHost)
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (isTransportSecurityEnabled) {
                item { FormSectionHeader(title = stringResource(R.string.editor_transport_security_settings)) }
                item {
                    FormCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = profileModel.streamSecurity.ifEmpty { editorSelectSecurity },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(editorTransportSecurityLabel) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    trailingIcon = {
                                        TrailingIcon(
                                            expanded = expanded
                                        )
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                        unfocusedTextColor = if (profileModel.streamSecurity.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    (if (isRealityEnabled) GlobalConst.transportSecurityList
                                    else GlobalConst.transportSecurityTlsOnlyList)
                                        .forEach { securityOption ->
                                            DropdownMenuItem(
                                                text = { Text(securityOption) },
                                                onClick = {
                                                    viewModel.updateProfileModel { currentState ->
                                                        currentState.copy(streamSecurity = securityOption)
                                                    }
                                                    expanded = false
                                                }
                                            )
                                        }
                                }
                            }
                            if (profileModel.streamSecurity == GlobalConst.transportSecurityTls
                                || profileModel.streamSecurity == GlobalConst.transportSecurityReality
                            ) {
                                EditableTrailingIconField(
                                    value = profileModel.sni,
                                    onValueChange = { newValue ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(sni = newValue)
                                        }
                                    },
                                    label = { Text(editorSniLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "sni",
                                            title = editorEditPrefix.format(editorSniLabel),
                                            initialValue = profileModel.sni,
                                            onConfirm = { newSni ->
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(sni = newSni)
                                                }
                                            }
                                        )
                                    }
                                )
                                var utlsFingerprintExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = utlsFingerprintExpanded,
                                    onExpandedChange = { utlsFingerprintExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = profileModel.utlsFingerprint.ifEmpty { editorSelectUtls },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(editorUtlsLabel) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                        trailingIcon = {
                                            TrailingIcon(
                                                expanded = utlsFingerprintExpanded
                                            )
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                            unfocusedTextColor = if (profileModel.utlsFingerprint.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = utlsFingerprintExpanded,
                                        onDismissRequest = { utlsFingerprintExpanded = false }
                                    ) {
                                        GlobalConst.utlsFingerprintList.forEach { fingerprintOption ->
                                            DropdownMenuItem(
                                                text = { Text(fingerprintOption) },
                                                onClick = {
                                                    viewModel.updateProfileModel { currentState ->
                                                        currentState.copy(utlsFingerprint = fingerprintOption)
                                                    }
                                                    utlsFingerprintExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (profileModel.streamSecurity == GlobalConst.transportSecurityTls) {
                                var alpnExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = alpnExpanded,
                                    onExpandedChange = { alpnExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = profileModel.alpn.ifEmpty { editorSelectAlpn },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(editorAlpnLabel) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                        trailingIcon = {
                                            TrailingIcon(
                                                expanded = alpnExpanded
                                            )
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                            unfocusedTextColor = if (profileModel.alpn.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = alpnExpanded,
                                        onDismissRequest = { alpnExpanded = false }
                                    ) {
                                        GlobalConst.alpnList.forEach { alpnOption ->
                                            DropdownMenuItem(
                                                text = { Text(alpnOption) },
                                                onClick = {
                                                    viewModel.updateProfileModel { currentState ->
                                                        currentState.copy(alpn = alpnOption)
                                                    }
                                                    alpnExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                var allowInsecureExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = allowInsecureExpanded,
                                    onExpandedChange = { allowInsecureExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = profileModel.allowInsecure.ifEmpty { editorSelectAllowInsecure },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(editorAllowInsecureLabel) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                        trailingIcon = {
                                            TrailingIcon(
                                                expanded = allowInsecureExpanded
                                            )
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                            unfocusedTextColor = if (profileModel.allowInsecure.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = allowInsecureExpanded,
                                        onDismissRequest = { allowInsecureExpanded = false }
                                    ) {
                                        GlobalConst.allowInsecureList.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    viewModel.updateProfileModel { currentState ->
                                                        currentState.copy(allowInsecure = option)
                                                    }
                                                    allowInsecureExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                EditableTrailingIconField(
                                    value = profileModel.echConfigList,
                                    onValueChange = { newValue ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(echConfigList = newValue)
                                        }
                                    },
                                    label = { Text(editorEchConfigLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "echConfigList",
                                            title = editorEditPrefix.format(editorEchConfigLabel),
                                            initialValue = profileModel.echConfigList,
                                            onConfirm = { newEchConfigList ->
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(echConfigList = newEchConfigList)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = profileModel.cert,
                                    onValueChange = { newValue ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(cert = newValue)
                                        }
                                    },
                                    label = { Text(editorClientCertLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "cert",
                                            title = editorEditPrefix.format(editorClientCertLabel),
                                            initialValue = profileModel.cert,
                                            onConfirm = { newCert ->
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(cert = newCert)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = profileModel.certVerifyName,
                                    onValueChange = { newValue ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(certVerifyName = newValue)
                                        }
                                    },
                                    label = { Text(editorVerifyPeerNameLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "certVerifyName",
                                            title = editorEditPrefix.format(
                                                editorVerifyPeerNameLabel
                                            ),
                                            initialValue = profileModel.certVerifyName,
                                            onConfirm = { newCertVerifyName ->
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(certVerifyName = newCertVerifyName)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = profileModel.certSha,
                                    onValueChange = { newValue ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(certSha = newValue)
                                        }
                                    },
                                    label = { Text(editorCertShaLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "certSha",
                                            title = editorEditPrefix.format(editorCertShaLabel),
                                            initialValue = profileModel.certSha,
                                            onConfirm = { newCertSha ->
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(certSha = newCertSha)
                                                }
                                            }
                                        )
                                    }
                                )
                            } else if (profileModel.streamSecurity == GlobalConst.transportSecurityReality
                                && isRealityEnabled
                            ) {
                                EditableTrailingIconField(
                                    value = profileModel.realityPublicKey,
                                    onValueChange = { newValue ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(realityPublicKey = newValue)
                                        }
                                    },
                                    label = { Text(editorRealityPublicKeyLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "realityPublicKey",
                                            title = editorEditPrefix.format(
                                                editorRealityPublicKeyLabel
                                            ),
                                            initialValue = profileModel.realityPublicKey,
                                            onConfirm = { newPublicKey ->
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(realityPublicKey = newPublicKey)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = profileModel.realityShortId,
                                    onValueChange = { newValue ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(realityShortId = newValue)
                                        }
                                    },
                                    label = { Text(editorRealityShortIdLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "realityShortId",
                                            title = editorEditPrefix.format(
                                                editorRealityShortIdLabel
                                            ),
                                            initialValue = profileModel.realityShortId,
                                            onConfirm = { newShortId ->
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(realityShortId = newShortId)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = profileModel.realitySpiderX,
                                    onValueChange = { newValue ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(realitySpiderX = newValue)
                                        }
                                    },
                                    label = { Text(editorRealitySpiderXLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "realitySpiderX",
                                            title = editorEditPrefix.format(
                                                editorRealitySpiderXLabel
                                            ),
                                            initialValue = profileModel.realitySpiderX,
                                            onConfirm = { newSpiderX ->
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(realitySpiderX = newSpiderX)
                                                }
                                            }
                                        )
                                    }
                                )
                                EditableTrailingIconField(
                                    value = profileModel.realityMldsa65Verify,
                                    onValueChange = { newValue ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(realityMldsa65Verify = newValue)
                                        }
                                    },
                                    label = { Text(editorRealityMldsa65Label) },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "realityMldsa65Verify",
                                            title = editorEditPrefix.format(
                                                editorRealityMldsa65Label
                                            ),
                                            initialValue = profileModel.realityMldsa65Verify,
                                            onConfirm = { newMldsa65Verify ->
                                                viewModel.updateProfileModel { currentState ->
                                                    currentState.copy(realityMldsa65Verify = newMldsa65Verify)
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item { FormSectionHeader(title = stringResource(R.string.editor_finalmask_settings)) }
            item {
                FormCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        EditableTrailingIconField(
                            value = profileModel.finalmask,
                            onValueChange = { newValue ->
                                viewModel.updateProfileModel { currentState ->
                                    currentState.copy(finalmask = newValue)
                                }
                            },
                            label = { Text(editorFinalmaskLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            onEditIconClick = {
                                activeDialogContext = FormBottomSheetContext(
                                    fieldKey = "finalmask",
                                    title = editorEditPrefix.format(editorFinalmaskLabel),
                                    initialValue = profileModel.finalmask,
                                    onConfirm = { newFinalmask ->
                                        viewModel.updateProfileModel { currentState ->
                                            currentState.copy(finalmask = newFinalmask)
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
    ReusableFormBottomSheet(
        context = activeDialogContext,
        onDismiss = {
            activeDialogContext = null
        }
    )
}

@Composable
fun FormSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}