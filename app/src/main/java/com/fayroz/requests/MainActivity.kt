package com.fayroz.requests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.fayroz.requests.ui.navigation.FayrozApp
import com.fayroz.requests.ui.theme.FayrozTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FayrozRequestsApp
        lifecycleScope.launch {
            app.ensureStarterCatalogOnce()
        }
        setContent {
            FayrozTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FayrozApp(repository = app.repository)
                }
            }
        }
    }
}
