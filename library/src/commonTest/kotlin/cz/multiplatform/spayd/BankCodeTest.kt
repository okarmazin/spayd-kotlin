// SPDX-License-Identifier: Apache-2.0

package cz.multiplatform.spayd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BankCodeTest {
    @Test
    fun `should create BankCode when input is valid 4 digit string`() {
        val result = BankCode.fromString("1234")
        val bankCode = result
        assertEquals("1234", bankCode.value)
    }

    @Test
    fun `should return error for invalid BankCode inputs`() {
        val inputs = listOf("123", "12345", "12a4", "")

        inputs.forEach { input ->
            val exception = assertFailsWith<IllegalArgumentException>("Failed for input - $input") {
                BankCode.fromString(input)
            }
            assertEquals(
                "Invalid bank code. Must be exactly 4 digits. Input has either bad length, or contains non-digits.",
                exception.message
            )
        }
    }

    @Test
    fun `test toString`() {
        val result = BankCode.fromString("1234")
        assertEquals("1234", result.toString())
    }
}
