package com.example.mediscannerai.data.local

import com.example.mediscannerai.domain.model.ParsedReport

/**
 * Temporary in-memory holder for the report currently being viewed.
 * Setting a new currentReport clears everything tied to the previous one,
 * so a new scan never shows an old explanation or looks "already saved".
 */
object ReportSessionHolder {
    var currentReport: ParsedReport? = null
        set(value) {
            field = value
            explanation = null
            doctorQuestions = null
            savedReportId = null
        }

    var explanation: String? = null
    var doctorQuestions: List<String>? = null
    var savedReportId: Long? = null
}