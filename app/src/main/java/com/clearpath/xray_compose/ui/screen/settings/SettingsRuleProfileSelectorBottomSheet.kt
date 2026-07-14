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
import com.clearpath.xray_compose.ui.components.ProfileSelector
import com.clearpath.xray_compose.ui.navigation.LocalNavigator
import com.clearpath.xray_compose.ui.navigation.sharedviewmodel.LocalSharedViewModelStoreOwner
import com.clearpath.xray_compose.viewmodel.SettingsRuleProfileSelectorViewModel
import com.clearpath.xray_compose.viewmodel.SettingsRuleViewModel

@Composable
fun SettingsRuleProfileSelectorBottomSheet() {
    val parentViewModel = hiltViewModel<SettingsRuleViewModel>(
        viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
    )
    val selectorViewModel = hiltViewModel<SettingsRuleProfileSelectorViewModel>()
    val navigator = LocalNavigator.current

    val rule by parentViewModel.ruleFlow.collectAsState()
    val ruleCustomOutboundRemark = rule.customOutboundRemark
    val selectedProfileRemark by selectorViewModel.selectedProfileRemark.collectAsState()

    LaunchedEffect(ruleCustomOutboundRemark) {
        selectorViewModel.setSelectedProfileRemark(ruleCustomOutboundRemark)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Row(
        //     modifier = Modifier
        //         .fillMaxWidth()
        //         .padding(16.dp),
        //     horizontalArrangement = Arrangement.spacedBy(12.dp)
        // ) {
        //     Text(
        //         text = "Select Profile",
        //         style = MaterialTheme.typography.titleLarge,
        //         maxLines = 1,
        //     )
        //     Spacer(modifier = Modifier.weight(1f))
        // }
        Text(
            text = "Select Profile",
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
                            customOutboundRemark = ""
                        )
                    }
                    selectorViewModel.setSelectedProfileRemark("")
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
                            customOutboundRemark = selectedProfileRemark
                        )
                    }
                    navigator.goBack()
                },
                enabled = selectedProfileRemark.isNotEmpty(),
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
        ProfileSelector(
            selectedProfileRemarks = if (selectedProfileRemark.isNotEmpty()) listOf(
                selectedProfileRemark
            ) else emptyList(),
            onProfileClicked = { profile ->
                selectorViewModel.setSelectedProfileRemark(profile.remark)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}