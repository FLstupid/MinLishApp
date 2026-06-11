package com.example.minlish

import com.example.minlish.logic.startOfDayMs
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    @Test
    fun startOfDayMs_returnsMidnight() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JUNE, 15, 14, 30, 45)
        cal.set(Calendar.MILLISECOND, 500)

        val result = startOfDayMs(cal.timeInMillis)

        val resultCal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun startOfDayMs_alreadyAtMidnight_unchanged() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JANUARY, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val epoch = cal.timeInMillis

        assertEquals(epoch, startOfDayMs(epoch))
    }
}
