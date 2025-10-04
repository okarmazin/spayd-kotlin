// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AmountTest {

    @Test
    fun `test Amount fromString with various inputs`() {
        // Group 1: Valid inputs with expected normalized outputs
        val validTestCases = mapOf(
            // Integers (should add .00)
            "0" to "0.00",
            "1" to "1.00",
            "123" to "123.00",
            "9999999" to "9999999.00",

            // Single decimal place (should pad to 2)
            "0.5" to "0.50",
            "1.0" to "1.00",
            "123.4" to "123.40",
            "9999.9" to "9999.90",

            // Two decimal places (should remain unchanged)
            "0.00" to "0.00",
            "0.01" to "0.01",
            "0.10" to "0.10",
            "1.23" to "1.23",
            "123.45" to "123.45",
            "9999.99" to "9999.99",

            // Edge cases with empty parts
            ".5" to "0.50",        // Empty integer part
            "0." to "0.00",        // Empty decimal part

            // Maximum allowed length (10 chars)
            "12345678.9" to "12345678.90",
            "1234567.89" to "1234567.89",
            "9999999.99" to "9999999.99"
        )

        // Test all other valid cases
        for ((input, expected) in validTestCases) {
            val amount = Amount.fromString(input)
            assertEquals(
                expected,
                amount.value,
                "For input '$input', expected '$expected' but got '${amount.value}'"
            )
        }

        // Group 2: Invalid inputs that should throw IllegalArgumentException
        val invalidTestCases = listOf(
            // Length issues
            "" to "AM: Amount must not exceed 10 characters and must have at least 1 digit",
            "." to "AM: Amount must not exceed 10 characters and must have at least 1 digit",
            "12345678901" to "AM: Amount must not exceed 10 characters and must have at least 1 digit",

            // Invalid characters
            "abc" to "AM: Amount must contain only digits or a decimal point",
            "12a" to "AM: Amount must contain only digits or a decimal point",
            "12.3a" to "AM: Amount must contain only digits or a decimal point",
            "12,34" to "AM: Amount must contain only digits or a decimal point", // Comma instead of decimal point

            // Too many decimal points
            "1.2.3" to "AM: Amount must contain at most one decimal point",
            "1.2.3.4" to "AM: Amount must contain at most one decimal point",

            // Too many decimal places
            "1.234" to "AM: Amount must be a decimal number with at most 2 decimal places.",
            "0.123" to "AM: Amount must be a decimal number with at most 2 decimal places.",
            "123.456" to "AM: Amount must be a decimal number with at most 2 decimal places."
        )

        for ((input, expectedErrorMessage) in invalidTestCases) {
            val exception = assertFailsWith<IllegalArgumentException>(
                message = "Expected exception for invalid input: '$input'"
            ) {
                Amount.fromString(input)
            }

            assertEquals(expectedErrorMessage, exception.message, "for input '$input'")
        }
    }

    @Test
    fun `test Amount edge cases`() {
        // Edge case: exactly 10 characters
        assertEquals("9999999.99", Amount.fromString("9999999.99").value)

        // Edge case: Zero
        assertEquals("0.00", Amount.fromString("0").value)
        assertEquals("0.00", Amount.fromString("0.0").value)
        assertEquals("0.00", Amount.fromString("0.00").value)

        // Edge case: Leading zeros
        assertEquals("000.10", Amount.fromString("000.1").value)
        assertEquals("0.10", Amount.fromString("0.10").value)

        // Edge case: Different decimal configurations
        assertEquals("1.00", Amount.fromString("1").value)
        assertEquals("1.00", Amount.fromString("1.").value)
        assertEquals("1.00", Amount.fromString("1.0").value)
        assertEquals("1.00", Amount.fromString("1.00").value)

        // Edge case: Boundary of max allowed length
        assertFailsWith<IllegalArgumentException> {
            Amount.fromString("12345678.90") // 11 characters
        }
    }
}
