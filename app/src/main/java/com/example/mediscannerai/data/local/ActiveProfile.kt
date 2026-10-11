package com.example.mediscannerai.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which person's data is on screen. Saved on the phone, and observable by every screen. */
object ActiveProfile {

    private const val FILE_NAME = "profile_prefs"
    private const val KEY_ACTIVE = "active_profile_id"

    private val _id = MutableStateFlow(1L)
    val id: StateFlow<Long> = _id.asStateFlow()

    fun load(context: Context) {
        _id.value = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_ACTIVE, 1L)
    }

    fun set(context: Context, profileId: Long) {
        _id.value = profileId
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_ACTIVE, profileId)
            .apply()
    }
}