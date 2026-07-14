package com.clearpath.xray_compose.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearpath.xray_compose.R
import com.clearpath.xray_compose.ui.components.AppSelector
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.sharedviewmodel.LocalSharedViewModelStoreOwner
import com.clearpath.xray_compose.viewmodel.SettingsRuleAppSelectorViewModel
import com.clearpath.xray_compose.viewmodel.SettingsRuleViewModel

@Composable
fun SettingsRuleAppSelectorBottomSheet() {
    val parentViewModel = hiltViewModel<SettingsRuleViewModel>(
        viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
    )
    val selectorViewModel = hiltViewModel<SettingsRuleAppSelectorViewModel>()
    val navigator = LocalNavigator.current

    val rule by parentViewModel.ruleFlow.collectAsState()
    val ruleProcess = rule.process
    val selectedAppPackageList by selectorViewModel.selectedAppPackageList.collectAsState()

    LaunchedEffect(ruleProcess) {
        selectorViewModel.setSelectedAppPackageList(ruleProcess)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Text(
            text = "Select App",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    // set null
                    parentViewModel.updateRule { rule ->
                        rule.copy(
                            process = emptyList()
                        )
                    }
                    selectorViewModel.setSelectedAppPackageList(emptyList())
                    navigator.goBack()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_deselect),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Clear")
            }
            Button(
                onClick = {
                    // set selected
                    parentViewModel.updateRule { rule ->
                        rule.copy(
                            process = selectedAppPackageList
                        )
                    }
                    navigator.goBack()
                },
                enabled = selectedAppPackageList.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Select")
            }
        }

        AppSelector(
            selectedPackageName = selectedAppPackageList,
            onSelectedChanged = { list ->
                selectorViewModel.setSelectedAppPackageList(list)
            },
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}