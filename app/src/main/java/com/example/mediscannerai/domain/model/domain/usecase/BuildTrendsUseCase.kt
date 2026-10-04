package com.example.mediscannerai.domain.usecase

import com.example.mediscannerai.data.local.ReportEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrendPoint(val timeMillis: Long, val value: Double, val dateLabel: String)

data class TrendSeries(
    val testName: String,
    val unit: String?,
    val referenceRange: String?,
    val points: List<TrendPoint>
)

/**
 * Builds a value-over-time series for every test found in the saved reports.
 * It re-reads each report's saved OCR text with the existing parser, so no
 * database change is needed. Each test has at most one reading per day: if
 * several saved reports give the same test on the same day, the most recently
 * saved one is used.
 */
class BuildTrendsUseCase(
    private val parser: ParseReportTextUseCase = ParseReportTextUseCase()
) {

    private class RawPoint(
        val display: String,
        val time: Long,
        val value: Double,
        val label: String,
        val unit: String?,
        val range: String?,
        val savedAt: Long
    )

    operator fun invoke(reports: List<ReportEntity>): List<TrendSeries> {
        val labelFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val byTest = linkedMapOf<String, MutableList<RawPoint>>()

        for (report in reports) {
            val parsed = parser(report.rawText)
            val time = parseReportDate(parsed.reportDate) ?: report.createdAt
            val label = labelFormat.format(Date(time))

            for (result in parsed.results) {
                if (result.testName == "Unidentified Test") continue
                val number = result.value?.toDoubleOrNull() ?: continue
                val normalized = normalize(result.testName)
                if (normalized.isBlank()) continue
                val key = ALIASES[normalized]?.first ?: normalized
                val display = ALIASES[normalized]?.second ?: result.testName.trim()
                byTest.getOrPut(key) { mutableListOf() }.add(
                    RawPoint(
                        display = display,
                        time = time,
                        value = number,
                        label = label,
                        unit = result.unit,
                        range = result.referenceRange,
                        savedAt = report.createdAt
                    )
                )
            }
        }

        return byTest.values.map { list ->
            // One reading per day: keep the one from the most recently saved report.
            val ordered = list
                .groupBy { it.label }
                .map { (_, sameDay) -> sameDay.maxBy { it.savedAt } }
                .sortedBy { it.time }
            TrendSeries(
                testName = ordered.last().display,
                unit = ordered.lastOrNull { it.unit != null }?.unit,
                referenceRange = ordered.lastOrNull { it.range != null }?.range,
                points = ordered.map { TrendPoint(it.time, it.value, it.label) }
            )
        }.sortedWith(
            compareByDescending<TrendSeries> { it.points.size }.thenBy { it.testName }
        )
    }

    private fun parseReportDate(text: String?): Long? {
        if (text == null) return null
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
                .parse(text)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun normalize(name: String): String =
        name.uppercase().filter { it.isLetterOrDigit() }

    private companion object {
        // normalised name -> (shared key, name shown to the user)
        val ALIASES: Map<String, Pair<String, String>> = buildMap {
            fun group(display: String, vararg names: String) {
                names.forEach { put(it, display.uppercase() to display) }
            }
            group("Haemoglobin", "HB", "HGB", "HAEMOGLOBIN", "HEMOGLOBIN")
            group("Ferritin", "FERRITIN", "SEFERRITIN", "SERUMFERRITIN")
            group("Platelet count", "PLT", "PLATELETS", "PLATELETCOUNT")
            group("Total leucocyte count", "TLC", "WBC", "TOTALLEUCOCYTECOUNT", "TOTALWBCCOUNT")
            group("Creatinine", "CREATININE", "SCREATININE", "SERUMCREATININE")
            group("Blood glucose", "GLUCOSE", "BLOODGLUCOSE", "FBS", "FASTINGGLUCOSE")
            group("Total cholesterol", "CHOLESTEROL", "TOTALCHOLESTEROL")
            group("Vitamin D", "VITAMIND", "VITD", "25OHVITAMIND")
        }
    }
}