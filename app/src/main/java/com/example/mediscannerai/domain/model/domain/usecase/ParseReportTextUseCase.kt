package com.example.mediscannerai.domain.usecase

import com.example.mediscannerai.domain.model.ParsedReport
import com.example.mediscannerai.domain.model.TestResult

private val RANGE_TOKEN_REGEX = Regex("""^\d+(\.\d+)?\s*[-~–]\s*\d+(\.\d+)?$""")
private val NUMBER_TOKEN_REGEX = Regex("""^\d+(\.\d+)?$""")
private val UNIT_TOKEN_REGEX = Regex("""^[A-Za-zµ/%]+$""")

private val RANGE_REGEX = Regex("""([\d.]+)\s*-\s*([\d.]+)""")
private val FLAG_VALUE_REGEX = Regex("""^([HL])\s*([\d.]+)$""")
private val FLAG_ONLY_REGEX = Regex("""^[HL]$""")
private val NUMBER_ONLY_REGEX = Regex("""^[\d.]+$""")
private val DATE_REGEX = Regex("""\d{2}/\d{2}/\d{4}""")

class ParseReportTextUseCase {

    operator fun invoke(rawText: String): ParsedReport {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val tabularResults = extractTabularResults(lines)
        val results = tabularResults.ifEmpty { extractSectionedResult(lines) }

        return ParsedReport(
            rawText = rawText,
            results = results,
            reportDate = extractDate(lines)
        )
    }

    /**
     * Handles reports laid out as a table (name, value, range, unit as
     * separate columns on one row). Splits each row on 2+ spaces — the
     * column separator produced by row reconstruction — then classifies
     * each piece by what it looks like, rather than expecting one rigid
     * pattern. Skips any row with no test name or no numeric value.
     */
    private fun extractTabularResults(lines: List<String>): List<TestResult> {
        val results = mutableListOf<TestResult>()
        for (line in lines) {
            val columns = line.split(Regex("\\s{2,}")).map { it.trim() }.filter { it.isNotBlank() }
            if (columns.size < 2) continue

            val name = columns.first().removeSuffix(":").trim()
            if (name.isBlank() || name.any { it.isDigit() }) continue

            var value: String? = null
            var unit: String? = null
            var range: String? = null

            for (column in columns.drop(1)) {
                when {
                    RANGE_TOKEN_REGEX.matches(column) && range == null -> range = column
                    NUMBER_TOKEN_REGEX.matches(column) && value == null -> value = column
                    UNIT_TOKEN_REGEX.matches(column) && unit == null -> unit = column
                }
            }

            if (value == null) continue
            results.add(TestResult(testName = name, value = value, unit = unit, referenceRange = range, flag = null))
        }
        return results
    }

    /**
     * Fallback for reports where the test name, flag, value, and reference range
     * are scattered across separate lines rather than one row. Best-effort —
     * returns an empty list rather than a guess when it can't find a value
     * with reasonable confidence.
     */
    private fun extractSectionedResult(lines: List<String>): List<TestResult> {
        var flag: String? = null
        var value: String? = null
        var referenceRange: String? = null
        var testName: String? = null

        for ((index, line) in lines.withIndex()) {
            FLAG_VALUE_REGEX.find(line)?.let {
                flag = it.groupValues[1]
                value = it.groupValues[2]
            }
            if (FLAG_ONLY_REGEX.matches(line) && index + 1 < lines.size) {
                val next = lines[index + 1]
                if (NUMBER_ONLY_REGEX.matches(next)) {
                    flag = line
                    value = next
                }
            }
            if (line.contains("Reference Value", ignoreCase = true) ||
                line.contains("Reference Range", ignoreCase = true)
            ) {
                val onSameLine = RANGE_REGEX.find(line)
                if (onSameLine != null) {
                    referenceRange = onSameLine.value
                } else if (index + 1 < lines.size && RANGE_REGEX.matches(lines[index + 1])) {
                    referenceRange = lines[index + 1]
                }
            }
            if (line.startsWith("Method", ignoreCase = true) && index > 0) {
                val candidate = lines[index - 1]
                if (candidate == candidate.uppercase() && candidate.length in 3..40) {
                    testName = candidate
                }
            }
        }

        if (value == null) return emptyList()

        return listOf(
            TestResult(
                testName = testName ?: "Unidentified Test",
                value = value,
                unit = null,
                referenceRange = referenceRange,
                flag = flag
            )
        )
    }

    private fun extractDate(lines: List<String>): String? {
        for (line in lines) {
            if (line.contains("Reported On", true) || line.contains("Collected On", true) ||
                line.contains("Registered On", true)
            ) {
                DATE_REGEX.find(line)?.let { return it.value }
            }
        }
        for (line in lines) {
            DATE_REGEX.find(line)?.let { return it.value }
        }
        return null
    }
}