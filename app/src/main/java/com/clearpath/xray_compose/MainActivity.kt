package com.clearpath.xray_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.clearpath.xray_compose.ui.navigation.AppNavHost
import com.clearpath.xray_compose.ui.theme.XrayComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XrayComposeTheme {
                AppNavHost()
            }
        }
    }
}
