package com.example.mediscannerai.data.local

import android.content.Context

/** Remembers whether the user has agreed to the "text is sent to an AI service" notice. */
object PrivacyPrefs {

    private const val FILE_NAME = "privacy_prefs"
    private const val KEY_AI_NOTICE = "ai_notice_accepted"

    fun hasAcceptedAiNotice(context: Context): Boolean =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AI_NOTICE, false)

    fun setAiNoticeAccepted(context: Context) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AI_NOTICE, true)
            .apply()
    }

    fun clearAiNoticeAccepted(context: Context) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AI_NOTICE, false)
            .apply()
    }
}