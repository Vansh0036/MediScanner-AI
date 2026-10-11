package com.example.mediscannerai.domain.usecase

enum class RangeStatus { BELOW, ABOVE, WITHIN, UNKNOWN }

/**
 * Compares a value with the reference range printed on the report.
 * Understands "12-18", "12 ~ 18", "12 to 18", "< 5", "<= 5", "> 40", ">= 40" and "up to 5".
 * Falls back to the report's own H or L flag, and otherwise says UNKNOWN. Never guesses.
 */
object RangeCheck {

    private val twoSided = Regex(
        """(\d+(?:\.\d+)?)\s*(?:-|~|\u2013|\u2014|to)\s*(\d+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE
    )
    private val oneSided = Regex(
        """(<=|>=|<|>|\u2264|\u2265|up\s*to|upto|less\s+than|more\s+than|greater\s+than|below|above)\s*(\d+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE
    )

    fun evaluate(value: String?, range: String?, flag: String?): RangeStatus {
        val v = value?.trim()?.toDoubleOrNull()
        if (v != null && !range.isNullOrBlank()) {
            twoSided.find(range)?.let { match ->
                val low = match.groupValues[1].toDouble()
                val high = match.groupValues[2].toDouble()
                if (low < high) {
                    return when {
                        v < low -> RangeStatus.BELOW
                        v > high -> RangeStatus.ABOVE
                        else -> RangeStatus.WITHIN
                    }
                }
            }
            oneSided.find(range)?.let { match ->
                val op = match.groupValues[1].lowercase().replace(Regex("\\s+"), "")
                val limit = match.groupValues[2].toDouble()
                return when (op) {
                    "<", "lessthan", "below" ->
                        if (v >= limit) RangeStatus.ABOVE else RangeStatus.WITHIN
                    "<=", "\u2264", "upto" ->
                        if (v > limit) RangeStatus.ABOVE else RangeStatus.WITHIN
                    ">", "morethan", "greaterthan", "above" ->
                        if (v <= limit) RangeStatus.BELOW else RangeStatus.WITHIN
                    ">=", "\u2265" ->
                        if (v < limit) RangeStatus.BELOW else RangeStatus.WITHIN
                    else -> RangeStatus.UNKNOWN
                }
            }
        }
        return when (flag?.trim()?.uppercase()) {
            "H" -> RangeStatus.ABOVE
            "L" -> RangeStatus.BELOW
            else -> RangeStatus.UNKNOWN
        }
    }
}