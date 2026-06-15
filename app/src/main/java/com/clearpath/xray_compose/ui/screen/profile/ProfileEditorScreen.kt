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
import androidx.compose.material3.TextButton
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

    // Port validator
    val portValidator: (String) -> String? = { value ->
        when {
            value.isEmpty() -> "Port cannot be empty"
            !value.all { it.isDigit() } -> "Port must contain only digits"
            value.toIntOrNull()
                ?.let { it !in 1..65535 } != false -> "Port must be between 1 and 65535"

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
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isNew) {
                            IconButton(onClick = {
                                viewModel.deleteProfile(onSuccess = {
                                    navigator.goBack()
                                }, onError = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to delete profile: $it")
                                    }
                                })
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = "Delete"
                                )
                            }
                        }
                        IconButton(onClick = {
                            viewModel.saveProfile(onSuccess = {
                                navigator.goBack()
                            }, onError = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to save profile: $it")
                                }
                            })
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_save),
                                contentDescription = "Save"
                            )
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Proto Extra: $protoExtra\nTransport Extra: $transportExtra")
                                }
                            }
                        ) {
                            Text("Test")
                        }
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
            item { FormSectionHeader(title = "Basic settings") }
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
                            label = { Text("Remark") },
                            modifier = Modifier.fillMaxWidth(),
                            onEditIconClick = {
                                activeDialogContext = FormBottomSheetContext(
                                    fieldKey = "remark",
                                    title = "Edit Remark",
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
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            onEditIconClick = {
                                activeDialogContext = FormBottomSheetContext(
                                    fieldKey = "address",
                                    title = "Edit Address",
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
                            label = { Text("Port") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            onEditIconClick = {
                                activeDialogContext = FormBottomSheetContext(
                                    fieldKey = "port",
                                    title = "Edit Port",
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
                item { FormSectionHeader(title = "VLESS settings") }
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
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = if (protoExtra.flow.isNullOrEmpty()) "Select Flow" else protoExtra.flow!!,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Flow") },
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
                                label = { Text("VLESS Encryption") },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "vlessEncryption",
                                        title = "Edit VLESS Encryption",
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
                item { FormSectionHeader(title = "HYSTERIA2 settings") }
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
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "hysteria2Password",
                                        title = "Edit Password",
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
                                label = { Text("Salamander Password") },
                                modifier = Modifier.fillMaxWidth(),
                                onEditIconClick = {
                                    activeDialogContext = FormBottomSheetContext(
                                        fieldKey = "hysteria2SalamanderPassword",
                                        title = "Edit Salamander Password",
                                        initialValue = profileModel.password,
                                        onConfirm = { newValue ->
                                            viewModel.updateProtocolExtra { currentState ->
                                                currentState.copy(salamanderPass = newValue)
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            } else if (profileModel.configType == EConfigType.TROJAN) {
                item { FormSectionHeader(title = "TROJAN settings") }
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
                item { FormSectionHeader(title = "Transport Methods settings") }
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
                                    label = { Text("Transport Methods") },
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
                                        label = { Text("XHTTP Mode") },
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
                                    label = { Text("XHTTP Host") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "xhttpHost",
                                            title = "Edit XHTTP Host",
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
                                    label = { Text("XHTTP Path") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "xhttpPath",
                                            title = "Edit XHTTP Path",
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
                                    label = { Text("XHTTP Extra") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "xhttpExtra",
                                            title = "Edit XHTTP Extra",
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
                                    label = { Text("Host") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "host",
                                            title = "Edit Host",
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
                                    label = { Text("Path") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "path",
                                            title = "Edit Path",
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
                                EditableTrailingIconField(
                                    value = transportExtra.grpcAuthority ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(grpcAuthority = newValue)
                                        }
                                    },
                                    label = { Text("Authority") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "authority",
                                            title = "Edit Authority",
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
                                    label = { Text("Service Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "serviceName",
                                            title = "Edit Service Name",
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
                                EditableTrailingIconField(
                                    value = transportExtra.kcpSeed ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateTransportExtra { currentState ->
                                            currentState.copy(kcpSeed = newValue)
                                        }
                                    },
                                    label = { Text("Seed") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "seed",
                                            title = "Edit Seed",
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
                item { FormSectionHeader(title = "Transport Security settings") }
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
                                    value = profileModel.streamSecurity.ifEmpty { "Select Transport Security" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Transport Security") },
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
                                    label = { Text("SNI") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "sni",
                                            title = "Edit SNI",
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
                                        value = profileModel.utlsFingerprint.ifEmpty { "Select uTLS Fingerprint" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("uTLS Fingerprint") },
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
                                        value = profileModel.alpn.ifEmpty { "Select ALPN" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("ALPN") },
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
                                        value = profileModel.allowInsecure.ifEmpty { "Select Allow Insecure" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Allow Insecure") },
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
                                    label = { Text("ECH Config List") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "echConfigList",
                                            title = "Edit ECH Config List",
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
                                    label = { Text("Client Certificate") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "cert",
                                            title = "Edit Client Certificate",
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
                                    label = { Text("Verify Peer Cert By Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "certVerifyName",
                                            title = "Edit Verify Peer Cert By Name",
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
                                    label = { Text("Client Certificate SHA-256") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "certSha",
                                            title = "Edit Client Certificate SHA-256",
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
                                    label = { Text("Reality Public Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "realityPublicKey",
                                            title = "Edit Reality Public Key",
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
                                    label = { Text("Reality Short ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "realityShortId",
                                            title = "Edit Reality Short ID",
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
                                    label = { Text("Reality SpiderX") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "realitySpiderX",
                                            title = "Edit Reality SpiderX",
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
                                    label = { Text("Reality mldsa65verify") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onEditIconClick = {
                                        activeDialogContext = FormBottomSheetContext(
                                            fieldKey = "realityMldsa65Verify",
                                            title = "Edit Reality mldsa65verify",
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
            item { FormSectionHeader(title = "Finalmask settings") }
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
                            label = { Text("Finalmask") },
                            modifier = Modifier.fillMaxWidth(),
                            onEditIconClick = {
                                activeDialogContext = FormBottomSheetContext(
                                    fieldKey = "finalmask",
                                    title = "Edit Finalmask",
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