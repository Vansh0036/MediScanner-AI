package com.example.mediscannerai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mediscannerai.presentation.navigation.MediScannerNavGraph
import com.example.mediscannerai.ui.theme.MediScannerAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MediScannerAITheme {
                MediScannerNavGraph()
            }
        }
    }
}