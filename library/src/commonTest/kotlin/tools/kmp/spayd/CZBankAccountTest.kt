// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CZBankAccountTest {
    private val VALID_NUM = "77628031"
    private val VALID_PREFIX = "721"
    private val VALID_CODE = "0710"
    private val VALID_FULL = "$VALID_PREFIX-$VALID_NUM/$VALID_CODE"

    @Test
    fun `valid account number and bank code`() {
        val result = CZBankAccount.fromString(VALID_FULL)
        assertEquals(VALID_FULL, result.toString())
    }

    @Test
    fun `valid input with extra spaces`() {
        val input = "  $VALID_FULL  "
        val result = CZBankAccount.fromString(input)
        assertEquals(VALID_FULL, result.toString())
    }

    @Test
    fun `valid account number without prefix`() {
        val input = "$VALID_NUM/$VALID_CODE"
        val result = CZBankAccount.fromString(input)
        assertEquals(input, result.toString())
    }

    @Test
    fun `invalid format - missing bank code`() {
        val inputs = setOf(VALID_NUM, "/", "$VALID_NUM/$VALID_CODE/$VALID_CODE", "")
        for (input in inputs) {
            assertFailsWith<IllegalArgumentException>("Failed for input: $input") {
                CZBankAccount.fromString(VALID_NUM)
            }
        }
    }
}
