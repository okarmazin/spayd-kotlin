// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CZBankAccountNumberTest {
    private val VALID_NUM = "77628031"
    private val VALID_PREFIX = "721"
    private val VALID_FULL = "$VALID_PREFIX-$VALID_NUM"

    @Test
    fun `fromString should reject invalid format`() {
        val invalids = setOf("", "-", "1", "12345678901", "$VALID_PREFIX-$VALID_NUM-$VALID_NUM")
        invalids.forEach {
            val ex =
                assertFailsWith<IllegalArgumentException>("Failed for input: $it") { CZBankAccountNumber.fromString(it) }
            assertEquals("Invalid CZ bank account number format.", ex.message)
        }
    }

    @Test
    fun `fromString should reject invalid checksum`() {
        val invalids = setOf("723", "$VALID_PREFIX-2436782", "1463-$VALID_NUM")
        invalids.forEach {
            val ex =
                assertFailsWith<IllegalArgumentException>("Failed for input: $it") { CZBankAccountNumber.fromString(it) }
            assertEquals("Invalid CZ bank account number - check digit is invalid.", ex.message)
        }

        val allZero = "000000"
        val ex = assertFailsWith<IllegalArgumentException>("Failed for input: $allZero") {
            CZBankAccountNumber.fromString(allZero)
        }
        assertEquals("Invalid CZ bank account number - must have at least 2 non-zero digits.", ex.message)
    }

    @Test
    fun `test toString`() {
        val prefixed =
            setOf(VALID_FULL, "0$VALID_PREFIX-$VALID_NUM", "$VALID_PREFIX-0$VALID_NUM", "0$VALID_PREFIX-0$VALID_NUM")
        for (input in prefixed) {
            var result = CZBankAccountNumber.fromString(input)
            assertEquals(VALID_PREFIX, result.prefix)
            assertEquals(VALID_NUM, result.accountNumber)
            assertEquals(VALID_FULL, result.toString())

            result = CZBankAccountNumber.fromString(result.toString())
            assertEquals(VALID_PREFIX, result.prefix)
            assertEquals(VALID_NUM, result.accountNumber)
            assertEquals(VALID_FULL, result.toString())
        }

        val unprefixed = setOf(VALID_NUM, "0$VALID_NUM")
        for (input in unprefixed) {
            var result = CZBankAccountNumber.fromString(input)
            assertEquals("", result.prefix)
            assertEquals(VALID_NUM, result.accountNumber)
            assertEquals(VALID_NUM, result.toString())

            result = CZBankAccountNumber.fromString(result.toString())
            assertEquals("", result.prefix)
            assertEquals(VALID_NUM, result.accountNumber)
            assertEquals(VALID_NUM, result.toString())
        }
    }
}
