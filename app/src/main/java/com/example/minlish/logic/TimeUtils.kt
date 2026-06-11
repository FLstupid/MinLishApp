package com.example.minlish.logic

import java.util.Calendar

/**
 * Returns the start of the day (midnight) for the given epoch milliseconds.
 */
fun startOfDayMs(epochMs: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMs
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
