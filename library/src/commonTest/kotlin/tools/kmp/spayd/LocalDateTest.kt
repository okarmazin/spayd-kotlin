// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalDateTest {

    @Test
    fun `test valid date strings are parsed correctly`() {
        // Test regular dates
        assertLocalDate(2023, 1, 1, "20230101")
        assertLocalDate(2023, 12, 31, "20231231")
        assertLocalDate(1900, 1, 1, "19000101")

        // Test leap years (February 29)
        assertLocalDate(2000, 2, 29, "20000229") // Leap year (divisible by 400)
        assertLocalDate(2004, 2, 29, "20040229") // Leap year (divisible by 4 but not 100)
        assertLocalDate(2020, 2, 29, "20200229") // Leap year

        // Test month boundaries
        assertLocalDate(2023, 1, 31, "20230131") // January has 31 days
        assertLocalDate(2023, 4, 30, "20230430") // April has 30 days
        assertLocalDate(2023, 2, 28, "20230228") // February in non-leap year has 28 days
    }

    @Test
    fun `test invalid date strings throw IllegalArgumentException`() {
        // Invalid format tests
        assertInvalidDate("", "DT: Date must be exactly 8 digits (YYYYMMDD).")
        assertInvalidDate("2023010", "DT: Date must be exactly 8 digits (YYYYMMDD).")
        assertInvalidDate("202301011", "DT: Date must be exactly 8 digits (YYYYMMDD).")
        assertInvalidDate("2023/01/01", "DT: Date must be exactly 8 digits (YYYYMMDD).")
        assertInvalidDate("abcdefgh", "DT: Date must be exactly 8 digits (YYYYMMDD).")

        // Invalid date value tests
        assertInvalidDate("18991231", "DT: Unreasonable year: 1899.")
        assertInvalidDate("18000101", "DT: Unreasonable year: 1800.")

        // Invalid month tests
        assertInvalidDate("20230000", "DT: Month number must be between 1 and 12.")
        assertInvalidDate("20231300", "DT: Month number must be between 1 and 12.")

        // Invalid day tests
        assertInvalidDate("20230100", "DT: Invalid day of month: 0")
        assertInvalidDate("20230132", "DT: Invalid day of month: 32")
        assertInvalidDate("20230431", "DT: Invalid day of month: 31") // April has 30 days

        // February special cases
        assertInvalidDate("20230229", "DT: Invalid day of month: 29") // Not a leap year
        assertInvalidDate("20230230", "DT: Invalid day of month: 30") // February never has 30 days
        assertInvalidDate("21000229", "DT: Invalid day of month: 29") // 2100 is not a leap year
    }

    // Helper functions
    private fun assertLocalDate(expectedYear: Int, expectedMonth: Int, expectedDay: Int, dateString: String) {
        val date = LocalDate.fromString("DT", dateString)
        assertEquals(expectedYear, date.year)
        assertEquals(expectedMonth, date.monthNumber)
        assertEquals(expectedDay, date.dayOfMonth)
    }

    private fun assertInvalidDate(dateString: String, expectedMessage: String) {
        val exception =
            assertFailsWith<SpaydException>("failed for input: $dateString") { LocalDate.fromString("DT", dateString) }

        assertEquals(expectedMessage, exception.message)
        println(exception.message)
    }
}
