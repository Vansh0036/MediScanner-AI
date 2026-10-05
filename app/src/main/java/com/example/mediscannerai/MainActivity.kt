package com.example.mediscannerai

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.mediscannerai.data.local.AppSettings
import com.example.mediscannerai.data.local.ReportMigration
import com.example.mediscannerai.data.local.ThemeMode
import com.example.mediscannerai.presentation.navigation.MediScannerNavGraph
import com.example.mediscannerai.presentation.splash.SplashOverlay
import com.example.mediscannerai.ui.theme.MediScannerAITheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.load(applicationContext)
        lifecycleScope.launch { ReportMigration.run(applicationContext) }

        setContent {
            val darkTheme = when (AppSettings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // Survives rotation, so the intro plays once per app start.
            var showSplash by rememberSaveable { mutableStateOf(true) }

            DisposableEffect(darkTheme) {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                    ) { darkTheme }
                )
                onDispose {}
            }

            MediScannerAITheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MediScannerNavGraph()
                    if (showSplash) {
                        SplashOverlay(onFinished = { showSplash = false })
                    }
                }
            }
        }
    }
}