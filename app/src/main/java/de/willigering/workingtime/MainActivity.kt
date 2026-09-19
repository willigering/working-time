package de.willigering.workingtime

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.willigering.workingtime.ui.WorkingTimeApp
import de.willigering.workingtime.ui.theme.WorkingTimeTrackerTheme
import de.willigering.workingtime.util.LocaleHelper

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkingTimeTrackerTheme {
                WorkingTimeApp()
            }
        }
    }
}