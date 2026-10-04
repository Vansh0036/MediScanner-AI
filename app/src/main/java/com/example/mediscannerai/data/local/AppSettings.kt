package com.example.mediscannerai.data.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { System, Light, Dark }

/** App-wide settings. The theme is Compose state, so changing it updates the UI at once. */
object AppSettings {

    private const val FILE_NAME = "app_settings"
    private const val KEY_THEME = "theme_mode"

    var themeMode by mutableStateOf(ThemeMode.System)
        private set

    fun load(context: Context) {
        val saved = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null)
        themeMode = ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.System
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.name)
            .apply()
    }
}