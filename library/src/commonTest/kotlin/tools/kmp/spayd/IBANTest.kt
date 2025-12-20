// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IBANTest {

    @Test
    fun `fromString with valid IBAN returns IBAN instance`() {
        val validIBAN = "DE89370400440532013000"
        val result = IBAN.fromString(validIBAN)
        assertEquals(validIBAN, result.value, "IBAN value does not match input string")
    }

    @Test
    fun `from cz bank account`() {
        val acc = CZBankAccount.fromParts(CZBankAccountNumber.fromString("7720-77628461"), BankCode.fromString("0710"))
        val result = IBAN.from(acc)
        val expected = "CZ2507100077200077628461"
        assertEquals(expected, result.value, "IBAN value does not match input string")
    }

    @Test
    fun `fromString with IBAN shorter than allowed length throws exception`() {
        val shortIBAN = "DE89"
        val exception = assertFailsWith<SpaydException> { IBAN.fromString(shortIBAN) }
        assertEquals("IBAN length (4) is not in the allowed range 16..34.", exception.message)
    }

    @Test
    fun `fromString with IBAN longer than allowed length throws exception`() {
        val longIBAN = "DE8937040044053201300012345678901234"
        val exception = assertFailsWith<SpaydException> { IBAN.fromString(longIBAN) }
        assertEquals("IBAN length (36) is not in the allowed range 16..34.", exception.message)
    }

    @Test
    fun `fromString with invalid country code throws exception`() {
        val invalidCountryCode = "1290370400440532013000"
        val exception = assertFailsWith<SpaydException> { IBAN.fromString(invalidCountryCode) }
        assertEquals("Invalid country code.", exception.message)
    }

    @Test
    fun `fromString with invalid check digits throws exception`() {
        val invalidCheckDigits = "DE8X370400440532013000"
        val exception = assertFailsWith<SpaydException> { IBAN.fromString(invalidCheckDigits) }
        assertEquals("Invalid check digits.", exception.message)
    }

    @Test
    fun `fromString with IBAN failing mod97 check throws exception`() {
        val invalidMod97IBAN = "DE89370400440532013001"

        val exception = assertFailsWith<SpaydException> { IBAN.fromString(invalidMod97IBAN) }
        assertEquals("Invalid IBAN: did not pass mod97 check.", exception.message)
    }
}
