// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AmountTest {

    @Test
    fun `test Amount fromString with various inputs`() {
        val validTestCases = mapOf(
            "0.00" to "0",
            "0.01" to "0.01",
            "0.10" to "0.1",
            "1.23" to "1.23",
            "1" to "1",
            "1.0" to "1",
            "123.45" to "123.45",
            "9999.99" to "9999.99",
            "9999.99000" to "9999.99",
            "9999.9000" to "9999.9",
            "00009999.9000" to "9999.9",
            "000000010000000.90000000000" to "10000000.9",
            "00000.0000" to "0",

            ".0" to "0",
            ".01" to "0.01",
            ".59" to "0.59",
            "0." to "0",
            "1." to "1",

            // Maximum allowed length (10 chars)
            "12345678.9" to "12345678.9",
            "1234567.89" to "1234567.89",
            "9999999.99" to "9999999.99",
            "1234567890" to "1234567890",
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
            "" to Amount.ERR_NAN,
            "." to Amount.ERR_SINGLE_DECIMAL_POINT,
            "12345678901" to Amount.ERR_TOO_LONG,
            "1234567890.0900" to Amount.ERR_TOO_LONG,

            "abc" to Amount.ERR_NAN,
            "12a" to Amount.ERR_NAN,
            "12.3a" to Amount.ERR_NAN,
            "12,34" to Amount.ERR_NAN,
            "1.2.3" to Amount.ERR_NAN,

            // Too many decimal places
            "1.234" to Amount.ERR_TOO_MANY_DECIMALS,
            "0.123" to Amount.ERR_TOO_MANY_DECIMALS,
            "123.456" to Amount.ERR_TOO_MANY_DECIMALS
        )

        for ((input, expectedErrorMessage) in invalidTestCases) {
            val exception = assertFailsWith<SpaydException>(
                message = "Expected exception for invalid input: '$input'"
            ) {
                Amount.fromString(input)
            }

            assertEquals(expectedErrorMessage, exception.message, "for input '$input'")
        }
    }
}
