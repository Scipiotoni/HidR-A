package com.hidra.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.hidra.launcher.ui.HidrATheme
import com.hidra.launcher.ui.LauncherRoot
import com.hidra.launcher.ui.MainViewModel

/**
 * Single-activity launcher. The manifest marks it as a HOME activity, so it can
 * be set as the device home screen; the app drawer icon/label are supplied by
 * whichever activity-alias is currently enabled (see DisguiseManager).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HidrATheme {
                LauncherRoot(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-scan when returning home so newly installed/removed apps show up.
        viewModel.refresh()
    }
}
