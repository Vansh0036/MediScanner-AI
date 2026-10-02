package com.example.mediscannerai.data.local

import com.example.mediscannerai.domain.model.ParsedReport

/**
 * Temporary in-memory holder for passing the current report between screens
 * without threading it through navigation arguments. Cleared automatically
 * if the app process dies. Replaced by Room-backed persistence in Phase 9.
 */
object ReportSessionHolder {
    var currentReport: ParsedReport? = null
}