package com.example.mediscannerai.domain.model

import java.time.DayOfWeek

/** The times of day a medicine can be linked to. Each slot has one clock time, set by the user. */
enum class MealSlot(val bit: Int, val label: String, val defaultMinutes: Int) {
    BEFORE_BREAKFAST(1, "Before breakfast", 7 * 60 + 30),
    AFTER_BREAKFAST(2, "After breakfast", 8 * 60 + 30),
    BEFORE_LUNCH(4, "Before lunch", 12 * 60 + 30),
    AFTER_LUNCH(8, "After lunch", 13 * 60 + 30),
    BEFORE_DINNER(16, "Before dinner", 19 * 60 + 30),
    AFTER_DINNER(32, "After dinner", 20 * 60 + 30),
    BEDTIME(64, "Bedtime", 22 * 60)
}

object ScheduleMasks {
    const val ALL_DAYS = 127

    /** Monday = 1, Tuesday = 2, Wednesday = 4 ... Sunday = 64. */
    fun dayBit(day: DayOfWeek): Int = 1 shl (day.value - 1)
}